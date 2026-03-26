package com.loopers.domain.event

import java.time.LocalDateTime

data class OrderCreatedEvent(
    val orderId: Long,
    val userId: Long,
    val productIds: List<Long>,
    val totalAmount: Long,
    val occurredAt: LocalDateTime = LocalDateTime.now(),
)
