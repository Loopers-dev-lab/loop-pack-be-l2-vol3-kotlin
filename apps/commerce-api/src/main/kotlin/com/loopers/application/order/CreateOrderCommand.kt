package com.loopers.application.order

data class CreateOrderCommand(
    val items: List<OrderItemCommand>,
)

data class OrderItemCommand(
    val productId: Long,
    val quantity: Int,
)
