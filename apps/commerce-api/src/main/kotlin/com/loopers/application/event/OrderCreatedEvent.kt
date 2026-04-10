package com.loopers.application.event

import java.math.BigDecimal

data class OrderCreatedEvent(
    val orderId: Long,
    val userId: Long,
    val productIds: List<Long>,
    val items: List<OrderItemEvent>,
    val totalAmount: BigDecimal,
    val couponId: Long?,
)

data class OrderItemEvent(
    val productId: Long,
    val unitPrice: BigDecimal,
    val quantity: Int,
)
