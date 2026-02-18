package com.loopers.interfaces.api.v1.order

import com.loopers.application.order.OrderInfo
import java.time.ZonedDateTime

data class GetOrderListResponse(
    val id: Long,
    val status: String,
    val totalAmount: Long,
    val orderedAt: ZonedDateTime,
    val itemCount: Int,
) {
    companion object {
        fun from(orderInfo: OrderInfo) = GetOrderListResponse(
            id = orderInfo.id,
            status = orderInfo.status,
            totalAmount = orderInfo.totalAmount,
            orderedAt = orderInfo.orderedAt,
            itemCount = orderInfo.items.size,
        )
    }
}
