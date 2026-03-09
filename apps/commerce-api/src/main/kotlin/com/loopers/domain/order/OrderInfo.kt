package com.loopers.domain.order

import java.math.BigDecimal
import java.time.ZonedDateTime

data class OrderInfo(
    val id: Long,
    val userId: Long,
    val status: OrderStatus,
    val totalPrice: BigDecimal,
    val createdAt: ZonedDateTime,
    val items: List<OrderItemInfo> = emptyList(),
) {
    companion object {
        fun from(model: OrderModel): OrderInfo {
            return OrderInfo(
                id = model.id,
                userId = model.userId,
                status = model.status,
                totalPrice = model.totalPrice,
                createdAt = model.createdAt,
            )
        }

        fun from(model: OrderModel, items: List<OrderItemInfo>): OrderInfo {
            return OrderInfo(
                id = model.id,
                userId = model.userId,
                status = model.status,
                totalPrice = model.totalPrice,
                createdAt = model.createdAt,
                items = items,
            )
        }
    }
}

data class OrderItemInfo(
    val id: Long,
    val orderId: Long,
    val productId: Long,
    val productName: String,
    val quantity: Int,
    val price: BigDecimal,
) {
    companion object {
        fun from(model: OrderItemModel): OrderItemInfo {
            return OrderItemInfo(
                id = model.id,
                orderId = model.orderId,
                productId = model.productId,
                productName = model.productName,
                quantity = model.quantity,
                price = model.price,
            )
        }
    }
}
