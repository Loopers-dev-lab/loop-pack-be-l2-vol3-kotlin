package com.loopers.application.order

import com.loopers.domain.order.Order
import com.loopers.domain.order.OrderItem
import java.time.ZonedDateTime

data class OrderDetailInfo(
    val orderId: Long,
    val totalAmount: Long,
    val status: String,
    val orderedAt: ZonedDateTime,
    val items: List<OrderItemInfo>,
) {
    companion object {
        fun from(order: Order, orderItems: List<OrderItem>): OrderDetailInfo {
            return OrderDetailInfo(
                orderId = order.id,
                totalAmount = order.totalAmount.amount,
                status = order.status.name,
                orderedAt = order.orderedAt,
                items = orderItems.map { OrderItemInfo.from(it) },
            )
        }
    }
}
