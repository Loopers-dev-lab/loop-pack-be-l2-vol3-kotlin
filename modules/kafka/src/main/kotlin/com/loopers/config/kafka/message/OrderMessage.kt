package com.loopers.config.kafka.message

import java.math.BigDecimal
import java.time.ZonedDateTime

data class OrderMessage(
    val eventId: String,
    val orderId: Long,
    val userId: Long,
    val totalPrice: BigDecimal,
    val items: List<OrderItemMessage>,
    val occurredAt: ZonedDateTime,
)

data class OrderItemMessage(
    val productId: Long,
    val quantity: Int,
    val price: BigDecimal,
)
