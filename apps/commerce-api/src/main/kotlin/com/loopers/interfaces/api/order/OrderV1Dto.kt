package com.loopers.interfaces.api.order

import com.loopers.application.order.OrderInfo
import com.loopers.application.order.OrderItemInfo

class OrderV1Dto {
    data class CreateOrderRequest(
        val items: List<OrderItemRequest>,
    )

    data class OrderItemRequest(
        val productId: Long,
        val quantity: Int,
    )

    data class OrderResponse(
        val id: Long,
        val totalPrice: Long,
        val items: List<OrderItemResponse>,
    ) {
        companion object {
            fun from(orderInfo: OrderInfo): OrderResponse {
                return OrderResponse(
                    id = orderInfo.id,
                    totalPrice = orderInfo.totalPrice,
                    items = orderInfo.items.map { OrderItemResponse.from(it) },
                )
            }
        }
    }

    data class OrderItemResponse(
        val productId: Long,
        val productName: String,
        val productPrice: Long,
        val quantity: Int,
    ) {
        companion object {
            fun from(itemInfo: OrderItemInfo): OrderItemResponse {
                return OrderItemResponse(
                    productId = itemInfo.productId,
                    productName = itemInfo.productName,
                    productPrice = itemInfo.productPrice,
                    quantity = itemInfo.quantity,
                )
            }
        }
    }
}
