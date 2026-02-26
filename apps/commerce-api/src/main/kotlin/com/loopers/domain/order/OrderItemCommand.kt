package com.loopers.domain.order

import com.loopers.domain.common.Money
import com.loopers.domain.common.Quantity

data class OrderItemCommand(
    val productId: Long,
    val quantity: Quantity,
    val productName: String,
    val productPrice: Money,
    val brandName: String,
)
