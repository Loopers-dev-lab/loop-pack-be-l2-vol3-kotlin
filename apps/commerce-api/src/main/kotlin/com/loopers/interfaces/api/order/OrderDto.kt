package com.loopers.interfaces.api.order

import com.loopers.application.order.OrderPlaceCommand

class OrderDto {

    data class PlaceOrderRequest(
        val items: List<OrderItemRequest>,
    ) {
        data class OrderItemRequest(
            val productId: Long,
            val quantity: Int,
        )

        fun toCommands(): List<OrderPlaceCommand> {
            return items.map { OrderPlaceCommand(productId = it.productId, quantity = it.quantity) }
        }
    }
}
