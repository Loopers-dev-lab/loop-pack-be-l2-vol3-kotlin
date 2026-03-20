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
        val receipt = receiptService.initiateReceipt(
            orderId = orderId,
            transactionId = transactionId,
            amount = orderInfo.amount,
            cardType = cardType,
            cardNo = cardNo,
        )

        try {
            // PG 요청 (타임아웃 가능)
            val pgResult = paymentClient.requestPayment(
                userId = userId,
                transactionId = transactionId,
                orderId = orderId,
                amount = orderInfo.amount,
                cardType = cardType,
                cardNo = cardNo,
            )

            // PG 응답 상태에 따라 Receipt 상태 결정
            when (pgResult.status.toString().uppercase()) {
                "COMPLETED" -> {
                    // 결제 성공 - Order 상태 변경
                    orderService.markOrderAsPaymentRequested(userId, orderId)
                    log.info("Payment completed: transactionId=$transactionId, orderId=$orderId")
                }
                "FAILED", "CANCELLED" -> {
                    // 결제 실패/취소 - Receipt 바로 FAILED로 변경
                    receiptService.markAsFailed(receipt, pgResult.reason)
                    log.warn("Payment failed: transactionId=$transactionId, reason=${pgResult.reason}")
                    throw CoreException(ErrorType.BAD_REQUEST, "결제가 실패하였습니다: ${pgResult.reason}")
                }
                "PENDING" -> {
                    // PG에서 아직 처리 중 - Receipt PENDING 유지, Order 상태 변경
                    orderService.markOrderAsPaymentRequested(userId, orderId)
                    log.info("Payment pending on PG side: transactionId=$transactionId")
                }
                else -> {
                    // 알 수 없는 상태
                    log.warn("Unknown payment status from PG: transactionId=$transactionId, status=${pgResult.status}")
                    throw CoreException(ErrorType.INTERNAL_ERROR, "PG 결제 응답이 올바르지 않습니다")
                }
            }
        } catch (e: Exception) {
            // 네트워크 타임아웃 또는 PG 서비스 오류
            // Receipt을 TIMEOUT 상태로 변경 → PaymentRecoveryService가 자동으로 복구 시도
            log.warn(
                "PG payment request failed (network timeout/error) for orderId=$orderId, transactionId=$transactionId. " +
                    "Receipt marked as TIMEOUT for automatic recovery. Cause: ${e.message}",
                e,
            )
            receiptService.markAsTimeout(receipt)
            throw CoreException(ErrorType.INTERNAL_ERROR, "PG 결제 요청에 실패했습니다. 잠시 후 다시 시도해주세요")
        }

        return ReceiptInfo.from(receipt)
    }

    private fun generateTransactionId(orderId: Long): String {
        return "TXN_${System.currentTimeMillis()}_$orderId"
    }
}
