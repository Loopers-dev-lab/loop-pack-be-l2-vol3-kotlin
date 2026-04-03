package com.loopers.application.user.order

class OrderCreateCommand(
    val userId: Long,
    val idempotencyKey: String,
    val items: List<Item>,
    val issuedCouponId: Long? = null,
    val entryToken: String? = null,
) {
    data class Item(val productId: Long, val quantity: Int)
}
