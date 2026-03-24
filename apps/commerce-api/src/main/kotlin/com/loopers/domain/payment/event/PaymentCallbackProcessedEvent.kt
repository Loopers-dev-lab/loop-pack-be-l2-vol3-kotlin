package com.loopers.domain.payment.event

data class PaymentCallbackProcessedEvent(
    val orderId: Long,
    val status: String,
    val transactionId: String? = null,
    val amount: Long? = null,
    val reason: String? = null,
    val dedupeKey: String =
        if (transactionId.isNullOrBlank()) {
            "payment.callback.processed:$orderId:$status"
        } else {
            "payment.callback.processed:$transactionId:$status"
        },
)
