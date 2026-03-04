package com.loopers.application.order

import com.loopers.domain.order.Order
import com.loopers.domain.order.OrderStatus
import java.time.ZonedDateTime

data class AdminOrderDetailInfo(
    val orderId: Long,
    val userId: Long,
    val totalAmount: Long,
    val discountAmount: Long,
    val paymentAmount: Long,
    val status: OrderStatus,
    val orderedAt: ZonedDateTime,
    val items: List<OrderItemInfo>,
) {
    companion object {
        fun from(order: Order): AdminOrderDetailInfo {
            return AdminOrderDetailInfo(
                orderId = order.id,
                userId = order.userId,
                totalAmount = order.totalAmount.value,
                discountAmount = order.discountAmount.value,
                paymentAmount = order.paymentAmount.value,
                status = order.status,
                orderedAt = order.createdAt,
                items = order.items.map { OrderItemInfo.from(it) },
            )
        }
    }
}
