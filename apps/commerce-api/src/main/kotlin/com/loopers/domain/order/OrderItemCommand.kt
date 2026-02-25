package com.loopers.domain.order

import com.loopers.domain.common.Money

data class OrderItemCommand(
    val productId: Long,
    val quantity: Int,
    val productName: String,
    val productPrice: Money,
    val brandName: String,
)
