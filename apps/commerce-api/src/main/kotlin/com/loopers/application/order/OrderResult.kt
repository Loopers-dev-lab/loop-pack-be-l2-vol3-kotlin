package com.loopers.application.order

import com.loopers.domain.Money
import com.loopers.domain.order.OrderInfo
import com.loopers.domain.order.OrderItemInfo
import java.time.ZonedDateTime

data class OrderResult(
    val id: Long,
    val userId: Long,
    val orderNumber: String,
    val couponIssueId: Long?,
    val originalTotalAmount: Money,
    val couponDiscountAmount: Money,
    val totalAmount: Money,
    val orderStatus: String,
    val items: List<OrderItemResult>,
    val createdAt: ZonedDateTime,
) {
    companion object {
        fun from(info: OrderInfo): OrderResult {
            return OrderResult(
                id = info.id,
                userId = info.userId,
                orderNumber = info.orderNumber,
                couponIssueId = info.couponIssueId,
                originalTotalAmount = info.originalTotalAmount,
                couponDiscountAmount = info.couponDiscountAmount,
                totalAmount = info.totalAmount,
                orderStatus = info.orderStatus,
                items = info.items.map { OrderItemResult.from(it) },
                createdAt = info.createdAt,
            )
        }
    }
}

data class OrderItemResult(
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
        fun from(info: OrderItemInfo): OrderItemResult {
            return OrderItemResult(
                id = info.id,
                productId = info.productId,
                quantity = info.quantity,
                itemStatus = info.itemStatus,
                productName = info.productName,
                brandName = info.brandName,
                brandId = info.brandId,
                imageUrl = info.imageUrl,
                originalPrice = info.originalPrice,
                discountAmount = info.discountAmount,
                finalPrice = info.finalPrice,
            )
        }
    }
}
