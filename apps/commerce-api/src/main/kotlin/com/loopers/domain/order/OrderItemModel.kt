package com.loopers.domain.order

data class OrderItemModel(
    val id: Long = 0,
    val productId: Long,
    val productName: String,
    val productPrice: Long,
    val brandName: String,
    val quantity: Int,
    val amount: Long = productPrice * quantity,
)
