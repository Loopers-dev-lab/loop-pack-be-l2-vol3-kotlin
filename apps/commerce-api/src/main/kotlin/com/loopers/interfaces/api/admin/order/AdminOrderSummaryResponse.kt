package com.loopers.interfaces.api.admin.order

import com.loopers.application.order.OrderSummaryInfo
import java.time.ZonedDateTime

data class AdminOrderSummaryResponse(
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
        fun from(info: OrderSummaryInfo): AdminOrderSummaryResponse {
            return AdminOrderSummaryResponse(
                orderId = info.orderId,
                userId = info.userId,
                originalAmount = info.originalAmount,
                discountAmount = info.discountAmount,
                totalAmount = info.totalAmount,
                status = info.status,
                orderedAt = info.orderedAt,
                itemCount = info.itemCount,
            )
        }
    }
}
