package com.loopers.interfaces.api.order

import com.loopers.application.order.OrderInfo
import java.time.ZonedDateTime

data class OrderCreateResponse(
    val orderId: Long,
    val totalAmount: Long,
    val status: String,
    val orderedAt: ZonedDateTime,
) {
    companion object {
        fun from(info: OrderInfo): OrderCreateResponse {
            return OrderCreateResponse(
                orderId = info.orderId,
                totalAmount = info.totalAmount,
                status = info.status,
                orderedAt = info.orderedAt,
            )
        }
    }
}
