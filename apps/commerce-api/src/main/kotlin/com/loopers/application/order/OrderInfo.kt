package com.loopers.application.order

import com.loopers.domain.order.Order
import com.loopers.domain.order.OrderStatus
import java.time.ZonedDateTime

data class OrderInfo(
    val orderId: Long,
    val totalAmount: Long,
    val status: OrderStatus,
    val orderedAt: ZonedDateTime,
) {
    companion object {
        fun from(order: Order): OrderInfo {
            return OrderInfo(
                orderId = order.id,
                totalAmount = order.totalAmount.value,
                status = order.status,
                orderedAt = order.createdAt,
            )
        }
    }
}
