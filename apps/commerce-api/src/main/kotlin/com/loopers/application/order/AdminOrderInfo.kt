package com.loopers.application.order

import com.loopers.domain.order.Order
import com.loopers.domain.order.OrderStatus
import java.time.ZonedDateTime

data class AdminOrderInfo(
    val orderId: Long,
    val userId: Long,
    val totalAmount: Long,
    val status: OrderStatus,
    val orderedAt: ZonedDateTime,
) {
    companion object {
        fun from(order: Order): AdminOrderInfo {
            return AdminOrderInfo(
                orderId = order.id,
                userId = order.userId,
                totalAmount = order.totalAmount.value,
                status = order.status,
                orderedAt = order.createdAt,
            )
        }
    }
}
