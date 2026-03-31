package com.loopers.application.order

class OrderCommand {
    data class CreateOrderItem(
        val productId: Long,
        val quantity: Int,
        val productName: String,
        val productPrice: Long,
        val brandName: String,
    )

    data class Create(
        val items: List<CreateItem>,
        val couponId: Long? = null,
    )

    data class CreateItem(
        val productId: Long,
        val quantity: Int,
    )
}
