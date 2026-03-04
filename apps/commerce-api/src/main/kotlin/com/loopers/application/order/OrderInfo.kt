package com.loopers.application.order

import com.loopers.domain.order.Order
import com.loopers.domain.order.OrderItem
import java.math.BigDecimal
import java.time.ZonedDateTime

data class OrderInfo(
    val id: Long,
    val userId: Long,
    val couponId: Long?,
    val originalAmount: BigDecimal,
    val discountAmount: BigDecimal,
    val totalAmount: BigDecimal,
    val items: List<OrderItemInfo>,
    val createdAt: ZonedDateTime,
) {
    companion object {
        fun from(order: Order): OrderInfo {
            return OrderInfo(
                id = order.id,
                userId = order.userId,
                couponId = order.couponId,
                originalAmount = order.originalAmount,
                discountAmount = order.discountAmount,
                totalAmount = order.totalAmount,
                items = order.orderItems.map { OrderItemInfo.from(it) },
                createdAt = order.createdAt,
            )
        }
    }
}

data class OrderItemInfo(
    val id: Long,
    val productId: Long,
    val productName: String,
    val brandName: String,
    val quantity: Int,
    val unitPrice: BigDecimal,
) {
    companion object {
        fun from(item: OrderItem): OrderItemInfo {
            return OrderItemInfo(
                id = item.id,
                productId = item.productId,
                productName = item.productName,
                brandName = item.brandName,
                quantity = item.quantity,
                unitPrice = item.unitPrice,
            )
        }
    }
}

data class ExcludedItemInfo(
    val productId: Long,
    val reason: String,
)

data class OrderResultInfo(
    val order: OrderInfo,
    val excludedItems: List<ExcludedItemInfo>,
) {
    companion object {
        fun of(order: Order, excludedItems: List<ExcludedItemInfo>): OrderResultInfo {
            return OrderResultInfo(
                order = OrderInfo.from(order),
                excludedItems = excludedItems,
            )
        }
    }
}
