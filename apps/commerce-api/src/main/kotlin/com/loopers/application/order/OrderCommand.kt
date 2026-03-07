package com.loopers.application.order

data class OrderItemCommand(
    val productId: Long,
    val quantity: Int,
)

data class PlaceOrderCommand(
    val items: List<OrderItemCommand>,
    val userCouponId: Long? = null,
)
