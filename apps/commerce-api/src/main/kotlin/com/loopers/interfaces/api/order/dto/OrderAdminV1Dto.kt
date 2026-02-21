package com.loopers.interfaces.api.order.dto

import com.loopers.domain.order.OrderDetail
import com.loopers.domain.order.entity.Order
import com.loopers.domain.order.entity.OrderItem
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
            fun from(orderDetail: OrderDetail): OrderAdminResponse {
                return OrderAdminResponse(
                    id = orderDetail.order.id,
                    userId = orderDetail.order.refUserId,
                    status = orderDetail.order.status,
                    totalPrice = orderDetail.order.totalPrice,
                    items = orderDetail.items.map { OrderItemAdminResponse.from(it) },
                    createdAt = orderDetail.order.createdAt,
                    updatedAt = orderDetail.order.updatedAt,
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
