package com.loopers.application.event

import java.math.BigDecimal

data class OrderCreatedEvent(
    val orderId: Long,
    val userId: Long,
    val productIds: List<Long>,
    val totalAmount: BigDecimal,
    val couponId: Long?,
)
