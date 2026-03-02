package com.loopers.domain.order

data class OrderLine(
    val productId: Long,
    val quantity: Quantity,
)
