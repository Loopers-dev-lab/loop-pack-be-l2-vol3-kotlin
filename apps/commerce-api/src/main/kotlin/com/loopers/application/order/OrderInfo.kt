package com.loopers.application.order

import com.loopers.domain.order.Order
import java.time.ZonedDateTime

data class OrderInfo(
    val orderId: Long,
    val originalAmount: Long,
    val discountAmount: Long,
    val totalAmount: Long,
    val status: String,
    val orderedAt: ZonedDateTime,
) {
    companion object {
        fun from(order: Order): OrderInfo {
            return OrderInfo(
                orderId = order.id,
                originalAmount = order.originalAmount.amount,
                discountAmount = order.discountAmount.amount,
                totalAmount = order.totalAmount.amount,
                status = order.status.name,
                orderedAt = order.orderedAt,
            )
        }
    }
}
