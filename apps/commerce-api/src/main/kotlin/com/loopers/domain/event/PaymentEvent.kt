package com.loopers.domain.event

import java.time.LocalDateTime

data class PaymentApprovedEvent(
    val paymentId: Long,
    val orderId: Long,
    val userId: Long,
    val amount: Long,
    val productIds: List<Long>,
    val occurredAt: LocalDateTime = LocalDateTime.now(),
)

data class PaymentFailedEvent(
    val paymentId: Long,
    val orderId: Long,
    val userId: Long,
    val reason: String,
    val occurredAt: LocalDateTime = LocalDateTime.now(),
)
