package com.loopers.domain.common.event

import java.math.BigDecimal

data class OrderCreatedEvent(
    val orderId: Long,
    val userId: Long,
    val loginId: String,
    val totalPrice: BigDecimal,
)
