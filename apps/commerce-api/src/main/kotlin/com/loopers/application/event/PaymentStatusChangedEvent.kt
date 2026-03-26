package com.loopers.application.event

data class PaymentStatusChangedEvent(
    val orderId: Long,
    val memberId: Long,
    val paymentStatus: String,
)
