package com.loopers.application.order

import com.loopers.domain.order.Order
import com.loopers.domain.order.OrderItem
import com.loopers.domain.order.OrderStatus
import java.time.ZonedDateTime

data class OrderDetailInfo(
    val orderId: Long,
    val userId: Long,
    val totalAmount: Long,
    val status: OrderStatus,
    val orderedAt: ZonedDateTime,
    val items: List<OrderItemInfo>,
) {
    companion object {
        fun from(order: Order): OrderDetailInfo {
            return OrderDetailInfo(
                orderId = order.id,
                userId = order.userId,
                totalAmount = order.totalAmount,
                status = order.status,
                orderedAt = order.createdAt,
                items = order.items.map { OrderItemInfo.from(it) },
            )
        }
    }
}

data class OrderItemInfo(
    val productId: Long,
    val quantity: Int,
    val productName: String,
    val productPrice: Long,
    val brandName: String,
) {
    companion object {
        fun from(orderItem: OrderItem): OrderItemInfo {
            return OrderItemInfo(
                productId = orderItem.productId,
                quantity = orderItem.quantity,
                productName = orderItem.productName,
                productPrice = orderItem.productPrice,
                brandName = orderItem.brandName,
            )
        }
    }
}
