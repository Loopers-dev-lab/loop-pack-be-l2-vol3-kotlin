package com.loopers.application.api.payment

import com.loopers.application.api.payment.dto.PaymentCallbackCommand
import com.loopers.domain.order.OrderService
import com.loopers.domain.payment.PaymentClient
import com.loopers.domain.payment.PaymentRequestResult
import com.loopers.domain.payment.Receipt
import com.loopers.domain.payment.ReceiptService
import com.loopers.domain.payment.dto.ReceiptInfo
import com.loopers.domain.payment.event.PaymentCallbackProcessedEvent
import com.loopers.domain.payment.event.PaymentRequestedEvent
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class PaymentFacade(
    private val receiptService: ReceiptService,
    private val orderService: OrderService,
    private val paymentClient: PaymentClient,
    private val eventPublisher: ApplicationEventPublisher,
) {

    @Transactional
    fun completePayment(command: PaymentCallbackCommand) {
        // (1) 결제 상태 업데이트
        receiptService.updateReceiptStatus(command)

        // (2) 주문 상태 변경 (이벤트로 분리)
        eventPublisher.publishEvent(
            PaymentCallbackProcessedEvent(
                orderId = command.orderId,
                status = command.status?.uppercase() ?: "UNKNOWN",
            ),
        )
    }

    @Transactional
    fun requestPayment(
        userId: Long,
        orderId: Long,
        cardType: String,
        cardNo: String,
    ): ReceiptInfo {
        // ✅ Pessimistic Lock: Order 행 잠금
        val orderInfo = orderService.getOrderInfoForPayment(userId, orderId)

        // ✅ Pessimistic Lock: Receipt 행 잠금 (동시 결제 요청 방지)
        val existingReceipt = receiptService.getReceiptByOrderIdForUpdate(orderId)
        if (existingReceipt != null) {
            receiptService.validateReceiptForNewPayment(existingReceipt)
        }

        val transactionId = generateTransactionId(orderId)

        val receipt = receiptService.initiateReceipt(
            orderId = orderId,
            transactionId = transactionId,
            amount = orderInfo.amount,
            cardType = cardType,
            cardNo = cardNo,
        )

        try {
            val pgResult = paymentClient.requestPayment(
                userId = userId,
                transactionId = transactionId,
                orderId = orderId,
                amount = orderInfo.amount,
                cardType = cardType,
                cardNo = cardNo,
            )

            handlePgResponse(pgResult, receipt, userId, orderId)
        } catch (e: Exception) {
            if (e is CoreException) throw e
            receiptService.markAsTimeout(receipt)
            throw CoreException(ErrorType.INTERNAL_ERROR, "PG 결제 요청에 실패했습니다. 잠시 후 다시 시도해주세요")
        }

        return ReceiptInfo.from(receipt)
    }

    private fun handlePgResponse(
        pgResult: PaymentRequestResult,
        receipt: Receipt,
        userId: Long,
        orderId: Long,
    ) {
        when (pgResult.status.toString().uppercase()) {
            "COMPLETED" -> {
                receiptService.markAsCompleted(receipt)
                // 주문 상태 변경 (이벤트로 분리)
                eventPublisher.publishEvent(
                    PaymentRequestedEvent(userId = userId, orderId = orderId),
                )
            }
            "FAILED", "CANCELLED" -> {
                receiptService.markAsFailed(receipt, pgResult.reason)
                throw CoreException(ErrorType.BAD_REQUEST, "결제가 실패하였습니다: ${pgResult.reason}")
            }
            "PENDING" -> {}
            else -> {
                throw CoreException(ErrorType.INTERNAL_ERROR, "PG 결제 응답이 올바르지 않습니다")
            }
        }
    }

    private fun generateTransactionId(orderId: Long): String {
        return "TXN_${System.currentTimeMillis()}_$orderId"
    }
}
