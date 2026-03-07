package com.loopers.application.order

import com.loopers.domain.order.Order
import com.loopers.domain.order.OrderItem

data class OrderInfo(
    val id: Long,
    val userId: Long,
    val originalTotalPrice: Long,
    val discountAmount: Long,
    val totalPrice: Long,
    val couponId: Long?,
    val items: List<OrderItemInfo>,
) {
    companion object {
        fun from(order: Order): OrderInfo {
            return OrderInfo(
                id = order.id,
                userId = order.userId,
                originalTotalPrice = order.originalTotalPrice,
                discountAmount = order.discountAmount,
                totalPrice = order.totalPrice,
                couponId = order.couponId,
                items = order.items.map { OrderItemInfo.from(it) },
            )
        }
    }
}

data class OrderItemInfo(
    val productId: Long,
    val productName: String,
    val productPrice: Long,
    val quantity: Int,
) {
    companion object {
        fun from(orderItem: OrderItem): OrderItemInfo {
            return OrderItemInfo(
                productId = orderItem.productId,
                productName = orderItem.productName,
                productPrice = orderItem.productPrice,
                quantity = orderItem.quantity,
            )
        }
    }
}
