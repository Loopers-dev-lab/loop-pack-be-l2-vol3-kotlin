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

        log.info("Payment completed successfully: orderId={}, transactionId={}", command.orderId, command.transactionId)
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
        val pgResult = try {
            paymentClient.requestPayment(
                userId = userId,
                transactionId = transactionId,
                orderId = orderId,
                amount = orderInfo.amount,
                cardType = cardType,
                cardNo = cardNo,
            )
        } catch (e: Exception) {
            throw CoreException(ErrorType.INTERNAL_ERROR, "PG 결제 요청에 실패했습니다")
        }

        // Order 상태를 PAYMENT_REQUESTED로 변경
        orderService.markOrderAsPaymentRequested(userId, orderId)

        // Receipt 생성 (PENDING 상태)
        val receipt = receiptService.initiateReceipt(
            orderId = orderId,
            transactionId = pgResult.transactionKey,
            amount = orderInfo.amount,
            cardType = cardType,
            cardNo = cardNo,
        )

        return ReceiptInfo.from(receipt)
    }

    private fun generateTransactionId(orderId: Long): String {
        return "TXN_${System.currentTimeMillis()}_$orderId"
    }
}
