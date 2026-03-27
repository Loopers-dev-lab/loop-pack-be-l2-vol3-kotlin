package com.loopers.interfaces.api.payment

import com.loopers.application.payment.PaymentCallbackCommand
import com.loopers.application.payment.PaymentCallbackStatus

class PaymentV1Dto {
    data class PgCallbackRequest(
        val orderId: Long,
        val transactionId: String?,
        val status: PaymentCallbackStatus,
        val failureReason: String?,
        val signature: String,
    ) {
        fun signaturePayload(): String {
            return listOf(
                orderId.toString(),
                status.name,
                transactionId ?: "",
                failureReason ?: "",
            ).joinToString("|")
        }

        fun toCommand(): PaymentCallbackCommand {
            return PaymentCallbackCommand(
                orderId = orderId,
                transactionId = transactionId,
                status = status,
                failureReason = failureReason,
            )
        }
    }
}
