package com.loopers.interfaces.api.order

import com.loopers.application.order.OrderItemResult
import com.loopers.application.order.OrderResult
import com.loopers.domain.order.Order
import com.loopers.domain.order.OrderItem

class OrderV1Dto {

    data class OrderItemRequest(
        val productId: Long,
        val quantity: Int,
    )

    data class PlaceOrderRequest(
        val items: List<OrderItemRequest>,
    )

    data class OrderItemResponse(
        val productId: Long,
        val productName: String,
        val brandId: Long,
        val brandName: String,
        val price: Int,
        val quantity: Int,
        val subtotal: Int,
    ) {
        companion object {
            fun from(result: OrderItemResult) = OrderItemResponse(
                productId = result.productId,
                productName = result.productName,
                brandId = result.brandId,
                brandName = result.brandName,
                price = result.price,
                quantity = result.quantity,
                subtotal = result.subtotal,
            )

            fun from(item: OrderItem) = OrderItemResponse(
                productId = item.productId,
                productName = item.productName,
                brandId = item.brandId,
                brandName = item.brandName,
                price = item.price,
                quantity = item.quantity,
                subtotal = item.subtotal(),
            )
        }
    }

    data class OrderResponse(
        val id: Long,
        val userId: Long,
        val totalPrice: Int,
        val items: List<OrderItemResponse>,
    ) {
        companion object {
            fun from(result: OrderResult) = OrderResponse(
                id = result.id,
                userId = result.userId,
                totalPrice = result.totalPrice,
                items = result.items.map { OrderItemResponse.from(it) },
            )

            fun from(order: Order) = OrderResponse(
                id = order.id,
                userId = order.userId,
                totalPrice = order.totalPrice,
                items = order.items.map { OrderItemResponse.from(it) },
            )
        }
    }
}
