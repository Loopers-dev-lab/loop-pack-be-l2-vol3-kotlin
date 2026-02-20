package com.loopers.domain.order

class OrderCommand {
    data class CreateOrder(val items: List<CreateOrderItem>)
    data class CreateOrderItem(val productId: Long, val quantity: Int)
}
