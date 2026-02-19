package com.loopers.domain.order

sealed interface OrderCommand {
    data class CreateOrder(val items: List<CreateOrderItem>) : OrderCommand
    data class CreateOrderItem(val productId: Long, val quantity: Int) : OrderCommand
}
