package com.loopers.application.order

data class PlaceOrderCommand(
    val items: List<PlaceOrderItemCommand>,
    val couponId: Long? = null,
) {
    data class PlaceOrderItemCommand(val productId: Long, val quantity: Int)
}
