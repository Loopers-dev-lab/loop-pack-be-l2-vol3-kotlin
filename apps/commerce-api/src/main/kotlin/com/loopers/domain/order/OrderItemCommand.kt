package com.loopers.domain.order

data class OrderItemCommand(
    val productId: Long,
    val quantity: Int,
)
