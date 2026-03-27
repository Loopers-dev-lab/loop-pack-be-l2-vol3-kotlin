package com.loopers.application.payment

import com.loopers.domain.payment.PaymentService
import com.loopers.infrastructure.payment.PgClient
import com.loopers.infrastructure.payment.PgPaymentRequest
import com.loopers.domain.payment.PaymentStatus
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component
import java.time.ZonedDateTime
import com.loopers.infrastructure.payment.PgPaymentStatus

@Component
class PaymentFacade(
    private val paymentService: PaymentService,
    private val pgClient: PgClient,
) {
    fun requestPayment(orderId: Long, amount: Long) {
        val payment = paymentService.createPayment(
            orderId = orderId,
            amount = amount,
            expiresAt = ZonedDateTime.now().plusMinutes(30),
        )

        val response = try {
            pgClient.requestPayment(
                PgPaymentRequest(
                    orderId = orderId,
                    amount = amount,
                ),
            )
        } catch (e: Exception) {
            val reason = e.message ?: "결제 요청에 실패했습니다."
            paymentService.markFailed(payment.id, reason)
            throw when (e) {
                is CoreException -> e
                else -> CoreException(ErrorType.INTERNAL_ERROR, reason)
            }
        }

        when (response.status) {
            PgPaymentStatus.APPROVED -> paymentService.markSucceeded(payment.id, response.transactionId)
            PgPaymentStatus.DEFERRED -> return
        }
    }

    fun handleCallback(command: PaymentCallbackCommand) {
        val payment = paymentService.findByOrderId(command.orderId)
        if (payment.status != PaymentStatus.PENDING) {
            return
        }

        when (command.status) {
            PaymentCallbackStatus.APPROVED -> {
                val transactionId = command.transactionId
                    ?: throw CoreException(ErrorType.BAD_REQUEST, "승인 콜백에는 transactionId가 필요합니다.")
                paymentService.markSucceeded(payment.id, transactionId)
            }

            PaymentCallbackStatus.FAILED -> {
                val failureReason = command.failureReason ?: "PG 결제에 실패했습니다."
                paymentService.markFailed(payment.id, failureReason)
            }
        }
    }

    fun expirePendingPayments(now: ZonedDateTime): Int {
        return paymentService.expirePendingPayments(now)
    }
}

data class PaymentCallbackCommand(
    val orderId: Long,
    val transactionId: String?,
    val status: PaymentCallbackStatus,
    val failureReason: String?,
)

enum class PaymentCallbackStatus {
    APPROVED,
    FAILED,
}
