package com.loopers.domain.order.dto

import java.math.BigDecimal

data class CreateOrderItemCommand(
    val productId: Long,
    val productName: String,
    val quantity: Int,
    val price: BigDecimal,
)
