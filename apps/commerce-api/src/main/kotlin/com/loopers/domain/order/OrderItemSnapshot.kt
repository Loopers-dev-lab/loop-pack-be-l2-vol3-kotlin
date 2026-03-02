package com.loopers.domain.order

import com.loopers.domain.product.Money

data class OrderItemSnapshot(
    val productId: Long,
    val productName: String,
    val productPrice: Money,
    val brandName: String,
    val imageUrl: String,
    val quantity: Quantity,
)
