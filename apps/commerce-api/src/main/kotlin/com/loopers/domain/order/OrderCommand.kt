package com.loopers.domain.order

class OrderCommand {
    data class CreateOrderItem(
        val productId: Long,
        val quantity: Int,
    )

    data class Create(
        val items: List<CreateOrderItem>,
    )
}
