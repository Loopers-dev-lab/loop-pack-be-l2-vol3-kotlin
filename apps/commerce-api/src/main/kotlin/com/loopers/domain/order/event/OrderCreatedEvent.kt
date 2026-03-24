package com.loopers.domain.order.event

data class OrderCreatedEvent(
    val orderId: Long,
    val userId: Long,
    val itemCount: Int,
    val couponId: Long?,
    val dedupeKey: String,
)
