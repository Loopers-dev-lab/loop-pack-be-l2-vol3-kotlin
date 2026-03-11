package com.loopers.application.order

data class PlaceOrderCommand(
    val items: List<PlaceOrderItemCommand>,
    val issuedCouponId: Long? = null,
) {
    data class PlaceOrderItemCommand(val productId: Long, val quantity: Int)
}
