package com.loopers.domain.order

import java.math.BigDecimal

data class OrderItemCommand(
    val productId: Long,
    val productName: String,
    val brandName: String,
    val quantity: Int,
    val unitPrice: BigDecimal,
)
