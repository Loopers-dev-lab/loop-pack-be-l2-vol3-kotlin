package com.loopers.application.api.payment

import com.loopers.application.api.payment.dto.PaymentCallbackCommand
import com.loopers.domain.order.OrderService
import com.loopers.domain.payment.PaymentClient
import com.loopers.domain.payment.PaymentRequestResult
import com.loopers.domain.payment.Receipt
import com.loopers.domain.payment.ReceiptStatus
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
        val receipt = receiptService.getReceiptByTransactionIdForUpdate(command.transactionId)

        // (1) 결제 상태 업데이트
        receiptService.updateReceiptStatus(command)

        when (receipt.status) {
            ReceiptStatus.COMPLETED -> orderService.markOrderAsPaid(command.orderId)
            ReceiptStatus.FAILED, ReceiptStatus.CANCELLED -> orderService.restoreOrderToPending(command.orderId)
            else -> {}
        }

        eventPublisher.publishEvent(
            PaymentCallbackProcessedEvent(
                transactionId = command.transactionId,
                orderId = command.orderId,
                amount = receipt.amount.toLong(),
                status = receipt.status.name,
                reason = command.reason,
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

            handlePgResponse(pgResult, receipt)
        } catch (e: Exception) {
            if (e is CoreException) throw e
            receiptService.markAsTimeout(receipt)
            throw CoreException(ErrorType.INTERNAL_ERROR, "PG 결제 요청에 실패했습니다. 잠시 후 다시 시도해주세요")
        }

        orderService.markOrderAsPaymentRequested(userId, orderId)
        eventPublisher.publishEvent(
            PaymentRequestedEvent(
                userId = userId,
                orderId = orderId,
                receiptId = receipt.id,
                transactionId = receipt.transactionId,
                amount = receipt.amount.toLong(),
                receiptStatus = receipt.status.name,
            ),
        )

        return ReceiptInfo.from(receipt)
    }

    private fun handlePgResponse(
        pgResult: PaymentRequestResult,
        receipt: Receipt,
    ) {
        when (pgResult.status.toString().uppercase()) {
            "COMPLETED" -> {
                receiptService.markAsCompleted(receipt)
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
