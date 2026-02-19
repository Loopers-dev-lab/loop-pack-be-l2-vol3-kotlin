package com.loopers.interfaces.api.order

import com.loopers.domain.order.Order
import com.loopers.domain.order.OrderCommand
import com.loopers.domain.order.OrderItem
import java.math.BigDecimal
import java.time.ZonedDateTime

class OrderV1Dto {
    data class CreateOrderRequest(
        val items: List<CreateOrderItemRequest>,
    ) {
        fun toCommand(): OrderCommand.CreateOrder {
            return OrderCommand.CreateOrder(
                items = items.map {
                    OrderCommand.CreateOrderItem(productId = it.productId, quantity = it.quantity)
                },
            )
        }
    }

    data class CreateOrderItemRequest(
        val productId: Long,
        val quantity: Int,
    )

    data class OrderResponse(
        val id: Long,
        val status: Order.OrderStatus,
        val totalPrice: BigDecimal,
        val items: List<OrderItemResponse>,
        val createdAt: ZonedDateTime,
    ) {
        companion object {
            fun from(order: Order): OrderResponse {
                return OrderResponse(
                    id = order.id,
                    status = order.status,
                    totalPrice = order.totalPrice,
                    items = order.items.map { OrderItemResponse.from(it) },
                    createdAt = order.createdAt,
                )
            }
        }
    }

    data class OrderItemResponse(
        val productId: Long,
        val productName: String,
        val productPrice: BigDecimal,
        val quantity: Int,
    ) {
        companion object {
            fun from(item: OrderItem): OrderItemResponse {
                return OrderItemResponse(
                    productId = item.refProductId,
                    productName = item.productName,
                    productPrice = item.productPrice,
                    quantity = item.quantity,
                )
            }
        }
    }
}
