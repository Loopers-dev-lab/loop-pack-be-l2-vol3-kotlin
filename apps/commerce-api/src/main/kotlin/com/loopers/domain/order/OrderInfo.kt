package com.loopers.domain.order

import java.math.BigDecimal
import java.time.ZonedDateTime

data class OrderInfo(
    val id: Long,
    val userId: Long,
    val status: OrderStatus,
    val originalPrice: BigDecimal,
    val discountAmount: BigDecimal,
    val totalPrice: BigDecimal,
    val createdAt: ZonedDateTime,
    val issuedCouponId: Long? = null,
    val items: List<OrderItemInfo> = emptyList(),
) {
    companion object {
        fun from(model: OrderModel): OrderInfo {
            return OrderInfo(
                id = model.id,
                userId = model.userId,
                status = model.status,
                originalPrice = model.originalPrice,
                discountAmount = model.discountAmount,
                totalPrice = model.totalPrice,
                createdAt = model.createdAt,
                issuedCouponId = model.issuedCouponId,
            )
        }

        fun from(model: OrderModel, items: List<OrderItemInfo>): OrderInfo {
            return OrderInfo(
                id = model.id,
                userId = model.userId,
                status = model.status,
                originalPrice = model.originalPrice,
                discountAmount = model.discountAmount,
                totalPrice = model.totalPrice,
                createdAt = model.createdAt,
                issuedCouponId = model.issuedCouponId,
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
