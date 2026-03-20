package com.loopers.interfaces.api.webhook.payment

import com.loopers.application.user.payment.PaymentCallbackCommand

class PaymentWebhookV1Request {

    data class Callback(
        val transactionKey: String,
        val status: String,
        val reason: String?,
    ) {
        fun toCommand(paymentId: Long): PaymentCallbackCommand = PaymentCallbackCommand(
            paymentId = paymentId,
            transactionKey = transactionKey,
            status = status,
            reason = reason,
        )
    }
}
