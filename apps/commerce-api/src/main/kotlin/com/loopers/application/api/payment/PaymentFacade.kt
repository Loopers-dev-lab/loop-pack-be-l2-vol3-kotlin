package com.loopers.application.api.payment

import com.loopers.application.api.payment.dto.PaymentCallbackCommand
import com.loopers.domain.order.OrderService
import com.loopers.domain.payment.PaymentClient
import com.loopers.domain.payment.ReceiptService
import com.loopers.domain.payment.dto.ReceiptInfo
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class PaymentFacade(
    private val receiptService: ReceiptService,
    private val orderService: OrderService,
    private val paymentClient: PaymentClient,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun completePayment(command: PaymentCallbackCommand) {
        // (1) 결제 상태 업데이트
        receiptService.updateReceiptStatus(command)

        // (2) 주문 상태 변경
        orderService.markOrderAsPaid(command.orderId)
    }

    @Transactional
    fun requestPayment(
        userId: Long,
        orderId: Long,
        cardType: String,
        cardNo: String,
    ): ReceiptInfo {
        val orderInfo = orderService.getOrderInfoForPayment(userId, orderId)

        val existingReceipt = receiptService.getReceiptByOrderId(orderId)
        if (existingReceipt != null) {
            receiptService.validateReceiptForNewPayment(existingReceipt)
        }

        val transactionId = generateTransactionId(orderId)

        // ✅ Receipt를 PG 요청 전에 먼저 생성 (PENDING 상태)
        // → PG 요청 실패해도 Receipt는 DB에 존재하여 복구 가능
        val receipt = receiptService.initiateReceipt(
            orderId = orderId,
            transactionId = transactionId,
            amount = orderInfo.amount,
            cardType = cardType,
            cardNo = cardNo,
        )

        try {
            // PG 요청 (타임아웃 가능)
            paymentClient.requestPayment(
                userId = userId,
                transactionId = transactionId,
                orderId = orderId,
                amount = orderInfo.amount,
                cardType = cardType,
                cardNo = cardNo,
            )
        } catch (e: Exception) {
            // PG 요청 실패 시에도 Receipt는 PENDING 상태로 유지
            // → PaymentRecoveryService가 자동으로 복구 시도
            log.warn(
                "PG payment request failed for orderId=$orderId, transactionId=$transactionId. " +
                    "Receipt left in PENDING state for automatic recovery. Cause: ${e.message}",
                e,
            )
            throw CoreException(ErrorType.INTERNAL_ERROR, "PG 결제 요청에 실패했습니다")
        }

        // Order 상태를 PAYMENT_REQUESTED로 변경
        orderService.markOrderAsPaymentRequested(userId, orderId)

        return ReceiptInfo.from(receipt)
    }

    private fun generateTransactionId(orderId: Long): String {
        return "TXN_${System.currentTimeMillis()}_$orderId"
    }
}
