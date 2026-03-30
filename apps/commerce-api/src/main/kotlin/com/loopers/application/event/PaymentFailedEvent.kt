package com.loopers.application.event

data class PaymentFailedEvent(
    val paymentId: Long,
    val orderId: Long,
    val userId: Long,
    val reason: String?,
)
