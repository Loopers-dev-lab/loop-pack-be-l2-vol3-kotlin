package com.loopers.domain.order.dto

import com.loopers.domain.order.Order
import java.math.BigDecimal
import java.time.ZonedDateTime

data class AdminOrderInfo(
    val orderId: Long,
    val userId: Long,
    val orderDate: ZonedDateTime,
    val totalPrice: BigDecimal,
    val orderItems: List<OrderedInfo.OrderedItemInfo>,
) {
    companion object {
        fun from(order: Order): AdminOrderInfo = AdminOrderInfo(
            orderId = order.id,
            userId = order.userId,
            orderDate = order.createdAt,
            totalPrice = order.getTotalPrice(),
            orderItems = order.orderItems.map { OrderedInfo.OrderedItemInfo.from(it) },
        )
    }
}
