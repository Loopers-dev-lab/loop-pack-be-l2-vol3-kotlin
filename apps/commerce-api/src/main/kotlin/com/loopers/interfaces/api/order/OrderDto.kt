package com.loopers.interfaces.api.order

import com.loopers.application.order.OrderInfo
import com.loopers.application.order.OrderPlaceCommand
import java.time.ZonedDateTime

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

    data class GetOrdersResponse(
        val orderId: Long,
        val totalAmount: Long,
        val status: String,
        val orderedAt: ZonedDateTime,
    ) {
        companion object {
            fun from(info: OrderInfo): GetOrdersResponse {
                return GetOrdersResponse(
                    orderId = info.orderId,
                    totalAmount = info.totalAmount,
                    status = info.status.name,
                    orderedAt = info.orderedAt,
                )
            }
        }
    }
}
