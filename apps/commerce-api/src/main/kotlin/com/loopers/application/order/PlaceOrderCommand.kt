package com.loopers.application.order

data class PlaceOrderCommand(
    val items: List<PlaceOrderItemCommand>,
) {
    data class PlaceOrderItemCommand(val productId: Long, val quantity: Int)
}
