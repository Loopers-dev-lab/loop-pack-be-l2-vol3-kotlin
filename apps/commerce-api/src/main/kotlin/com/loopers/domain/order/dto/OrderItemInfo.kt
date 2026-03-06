package com.loopers.domain.order.dto

import com.loopers.domain.order.OrderItem
import java.math.BigDecimal

data class OrderItemInfo(
    val productId: Long,
    val productName: String,
    val price: BigDecimal,
    val quantity: Int,
    val subtotal: BigDecimal,
) {
    companion object {
        fun from(orderItem: OrderItem): OrderItemInfo = OrderItemInfo(
            productId = orderItem.productId,
            productName = orderItem.productName,
            price = orderItem.price,
            quantity = orderItem.quantity,
            subtotal = orderItem.getSubtotal(),
        )
    }
}
