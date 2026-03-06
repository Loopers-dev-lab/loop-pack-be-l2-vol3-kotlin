package com.loopers.application.order

import com.loopers.domain.order.Order
import java.time.ZonedDateTime

data class OrderSummaryInfo(
    val orderId: Long,
    val userId: Long,
    val originalAmount: Long,
    val discountAmount: Long,
    val totalAmount: Long,
    val status: String,
    val orderedAt: ZonedDateTime,
    val itemCount: Int,
) {
    companion object {
        fun from(order: Order, itemCount: Int): OrderSummaryInfo {
            return OrderSummaryInfo(
                orderId = order.id,
                userId = order.userId,
                originalAmount = order.originalAmount.amount,
                discountAmount = order.discountAmount.amount,
                totalAmount = order.totalAmount.amount,
                status = order.status.name,
                orderedAt = order.orderedAt,
                itemCount = itemCount,
            )
        }
    }
}
