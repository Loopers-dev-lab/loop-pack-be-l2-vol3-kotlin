package com.loopers.application.order

class OrderCommand {
    data class CreateOrderItem(
        val productId: Long,
        val quantity: Int,
    )

    data class Create(
        val items: List<CreateOrderItem>,
    )
}
