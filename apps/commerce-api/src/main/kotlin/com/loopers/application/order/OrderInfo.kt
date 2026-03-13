package com.loopers.application.order

import com.loopers.domain.order.OrderItemModel
import com.loopers.domain.order.OrderModel
import com.loopers.domain.order.OrderStatus
import java.time.ZonedDateTime

data class OrderInfo(
    val id: Long,
    val userId: Long,
    val orderStatus: OrderStatus,
    val couponIssueId: Long?,
    val originalTotalAmount: Long,
    val discountAmount: Long,
    val totalAmount: Long,
    val orderItems: List<OrderItemInfo>,
    val createdAt: ZonedDateTime?,
) {
    companion object {
        fun from(order: OrderModel): OrderInfo {
            return OrderInfo(
                id = order.id,
                userId = order.userId,
                orderStatus = order.orderStatus,
                couponIssueId = order.couponIssueId,
                originalTotalAmount = order.originalTotalAmount,
                discountAmount = order.discountAmount,
                totalAmount = order.totalAmount,
                orderItems = order.orderItems.map { OrderItemInfo.from(it) },
                createdAt = runCatching { order.createdAt }.getOrNull(),
            )
        }
    }
}

data class OrderItemInfo(
    val id: Long,
    val productId: Long,
    val productName: String,
    val brandName: String,
    val price: Long,
    val quantity: Int,
    val subTotal: Long,
) {
    companion object {
        fun from(item: OrderItemModel): OrderItemInfo {
            return OrderItemInfo(
                id = item.id,
                productId = item.productId,
                productName = item.productName,
                brandName = item.brandName,
                price = item.price,
                quantity = item.quantity,
                subTotal = item.subTotal,
            )
        }
    }
}
