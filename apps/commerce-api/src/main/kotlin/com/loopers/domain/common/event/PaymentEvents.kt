package com.loopers.domain.common.event

data class PaymentRequestedEvent(
    val paymentId: Long,
    val orderId: Long,
    val memberId: Long,
    val amount: Long,
)

data class PaymentSucceededEvent(
    val paymentId: Long,
    val orderId: Long,
    val memberId: Long,
    val transactionKey: String,
)

data class PaymentFailedEvent(
    val paymentId: Long,
    val orderId: Long,
    val memberId: Long,
    val failReason: String?,
)
