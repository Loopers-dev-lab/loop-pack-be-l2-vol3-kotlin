package com.loopers.domain.order

data class OrderItemCommand(
    val productId: Long,
    val quantity: Int,
    val productName: String,
    val productPrice: Long,
    val brandName: String,
)
