package com.loopers.domain.order

import com.loopers.domain.Money
import java.time.ZonedDateTime

data class OrderInfo(
    val id: Long,
    val userId: Long,
    val orderNumber: String,
    val totalAmount: Money,
    val orderStatus: String,
    val items: List<OrderItemInfo>,
    val createdAt: ZonedDateTime,
) {
    companion object {
        fun from(order: Order): OrderInfo {
            return OrderInfo(
                id = order.id,
                userId = order.userId,
                orderNumber = order.orderNumber,
                totalAmount = order.totalAmount,
                orderStatus = order.orderStatus.name,
                items = order.orderItems.map { OrderItemInfo.from(it) },
                createdAt = order.createdAt,
            )
        }
    }
}

data class OrderItemInfo(
    val id: Long,
    val productId: Long,
    val quantity: Int,
    val itemStatus: String,
    val productName: String,
    val brandName: String,
    val brandId: Long,
    val imageUrl: String?,
    val originalPrice: Money,
    val discountAmount: Money,
    val finalPrice: Money,
) {
    companion object {
        fun from(orderItem: OrderItem): OrderItemInfo {
            return OrderItemInfo(
                id = orderItem.id,
                productId = orderItem.productId,
                quantity = orderItem.quantity,
                itemStatus = orderItem.itemStatus.name,
                productName = orderItem.productSnapshot.productName,
                brandName = orderItem.productSnapshot.brandName,
                brandId = orderItem.productSnapshot.brandId,
                imageUrl = orderItem.productSnapshot.imageUrl,
                originalPrice = orderItem.priceSnapshot.originalPrice,
                discountAmount = orderItem.priceSnapshot.discountAmount,
                finalPrice = orderItem.priceSnapshot.finalPrice,
            )
        }
    }
}
