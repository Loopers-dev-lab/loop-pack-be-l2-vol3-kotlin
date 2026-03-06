package com.loopers.application.admin.order

import com.loopers.domain.order.Order
import com.loopers.domain.order.OrderItem
import java.math.BigDecimal
import java.time.ZonedDateTime

class AdminOrderResult {

    data class Detail(
        val orderId: Long,
        val userId: Long,
        val status: String,
        val items: List<OrderItemDetail>,
        val totalAmount: BigDecimal,
        val discountAmount: BigDecimal,
        val finalAmount: BigDecimal,
        val createdAt: ZonedDateTime,
    ) {
        companion object {
            fun from(order: Order): Detail = Detail(
                orderId = order.id!!,
                userId = order.userId,
                status = order.status.name,
                items = order.items.map { OrderItemDetail.from(it) },
                totalAmount = order.totalAmount().amount,
                discountAmount = order.discountAmount.amount,
                finalAmount = order.finalAmount().amount,
                createdAt = order.createdAt!!,
            )
        }
    }

    data class OrderItemDetail(
        val productId: Long,
        val productName: String,
        val brandName: String,
        val regularPrice: BigDecimal,
        val sellingPrice: BigDecimal,
        val thumbnailUrl: String?,
        val quantity: Int,
    ) {
        companion object {
            fun from(orderItem: OrderItem): OrderItemDetail = OrderItemDetail(
                productId = orderItem.snapshot.productId,
                productName = orderItem.snapshot.productName,
                brandName = orderItem.snapshot.brandName,
                regularPrice = orderItem.snapshot.regularPrice.amount,
                sellingPrice = orderItem.snapshot.sellingPrice.amount,
                thumbnailUrl = orderItem.snapshot.thumbnailUrl,
                quantity = orderItem.quantity.value,
            )
        }
    }

    data class ListItem(
        val orderId: Long,
        val userId: Long,
        val status: String,
        val orderSummary: String,
        val itemCount: Int,
        val totalAmount: BigDecimal,
        val discountAmount: BigDecimal,
        val finalAmount: BigDecimal,
        val createdAt: ZonedDateTime,
    ) {
        companion object {
            fun from(order: Order): ListItem {
                val firstItemName = order.items.first().snapshot.productName
                val itemCount = order.items.size
                val orderSummary = if (itemCount == 1) {
                    firstItemName
                } else {
                    "$firstItemName 외 ${itemCount - 1}건"
                }
                return ListItem(
                    orderId = order.id!!,
                    userId = order.userId,
                    status = order.status.name,
                    orderSummary = orderSummary,
                    itemCount = itemCount,
                    totalAmount = order.totalAmount().amount,
                    discountAmount = order.discountAmount.amount,
                    finalAmount = order.finalAmount().amount,
                    createdAt = order.createdAt!!,
                )
            }
        }
    }
}
