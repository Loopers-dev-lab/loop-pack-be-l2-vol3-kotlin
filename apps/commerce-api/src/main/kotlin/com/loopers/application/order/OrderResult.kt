package com.loopers.application.order

import com.loopers.domain.order.Order
import com.loopers.domain.order.OrderItem

data class OrderItemResult(
    val id: Long,
    val productId: Long,
    val productName: String,
    val brandId: Long,
    val brandName: String,
    val price: Int,
    val quantity: Int,
    val subtotal: Int,
) {
    companion object {
        fun from(item: OrderItem): OrderItemResult = OrderItemResult(
            id = item.id,
            productId = item.productId,
            productName = item.productName,
            brandId = item.brandId,
            brandName = item.brandName,
            price = item.price,
            quantity = item.quantity,
            subtotal = item.subtotal(),
        )
    }
}

data class OrderResult(
    val id: Long,
    val userId: Long,
    val totalPrice: Int,
    val items: List<OrderItemResult>,
) {
    companion object {
        fun from(order: Order): OrderResult = OrderResult(
            id = order.id,
            userId = order.userId,
            totalPrice = order.totalPrice,
            items = order.items.map { OrderItemResult.from(it) },
        )
    }
}
