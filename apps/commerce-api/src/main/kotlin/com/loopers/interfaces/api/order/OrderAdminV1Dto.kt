package com.loopers.interfaces.api.order

import com.loopers.domain.order.Order
import com.loopers.domain.order.OrderItem
import java.math.BigDecimal
import java.time.ZonedDateTime

class OrderAdminV1Dto {
    data class OrderAdminResponse(
        val id: Long,
        val userId: Long,
        val status: Order.OrderStatus,
        val totalPrice: BigDecimal,
        val items: List<OrderItemAdminResponse>,
        val createdAt: ZonedDateTime,
        val updatedAt: ZonedDateTime,
    ) {
        companion object {
            fun from(order: Order): OrderAdminResponse {
                return OrderAdminResponse(
                    id = order.id,
                    userId = order.refUserId,
                    status = order.status,
                    totalPrice = order.totalPrice,
                    items = order.items.map { OrderItemAdminResponse.from(it) },
                    createdAt = order.createdAt,
                    updatedAt = order.updatedAt,
                )
            }
        }
    }

    data class OrderItemAdminResponse(
        val id: Long,
        val productId: Long,
        val productName: String,
        val productPrice: BigDecimal,
        val quantity: Int,
    ) {
        companion object {
            fun from(item: OrderItem): OrderItemAdminResponse {
                return OrderItemAdminResponse(
                    id = item.id,
                    productId = item.refProductId,
                    productName = item.productName,
                    productPrice = item.productPrice,
                    quantity = item.quantity,
                )
            }
        }
    }
}
