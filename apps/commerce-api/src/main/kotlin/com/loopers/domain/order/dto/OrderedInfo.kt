package com.loopers.domain.order.dto

import com.loopers.domain.order.Order
import com.loopers.domain.order.OrderItem
import java.math.BigDecimal
import java.time.ZonedDateTime

data class OrderedInfo(
    val orderId: Long,
    val orderDate: ZonedDateTime,
    val totalPrice: BigDecimal,
    val orderItems: List<OrderedItemInfo>,
) {
    companion object {
        fun from(order: Order): OrderedInfo = OrderedInfo(
            orderId = order.id,
            orderDate = order.createdAt,
            totalPrice = order.getTotalPrice(),
            orderItems = order.orderItems.map { OrderedItemInfo.from(it) },
        )
    }

    data class OrderedItemInfo(
        val productId: Long,
        val productName: String,
        val price: BigDecimal,
        val quantity: Int,
        val subtotal: BigDecimal,
    ) {
        companion object {
            fun from(orderItem: OrderItem): OrderedItemInfo = OrderedItemInfo(
                productId = orderItem.productId,
                productName = orderItem.productName,
                price = orderItem.price,
                quantity = orderItem.quantity,
                subtotal = orderItem.getSubtotal(),
            )
        }
    }
}
