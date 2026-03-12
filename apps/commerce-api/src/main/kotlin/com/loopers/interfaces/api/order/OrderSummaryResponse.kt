package com.loopers.interfaces.api.order

import com.loopers.application.order.OrderSummaryInfo
import java.time.ZonedDateTime

data class OrderSummaryResponse(
    val orderId: Long,
    val originalAmount: Long,
    val discountAmount: Long,
    val totalAmount: Long,
    val status: String,
    val orderedAt: ZonedDateTime,
    val itemCount: Int,
) {
    companion object {
        fun from(info: OrderSummaryInfo): OrderSummaryResponse {
            return OrderSummaryResponse(
                orderId = info.orderId,
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
