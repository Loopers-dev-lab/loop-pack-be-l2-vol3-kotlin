package com.loopers.domain.payment.event

data class PaymentRequestedEvent(
    val userId: Long,
    val orderId: Long,
    val receiptId: Long? = null,
    val transactionId: String? = null,
    val amount: Long? = null,
    val receiptStatus: String? = null,
    val dedupeKey: String =
        if (transactionId.isNullOrBlank()) {
            "payment.requested:$userId:$orderId"
        } else {
            "payment.requested:$userId:$transactionId"
        },
)
