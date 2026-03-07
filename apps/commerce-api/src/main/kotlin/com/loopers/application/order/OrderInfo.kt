package com.loopers.application.order

import com.loopers.domain.order.OrderItemModel
import com.loopers.domain.order.OrderModel
import java.time.ZonedDateTime

data class OrderInfo(
    val id: Long,
    val orderNumber: String,
    val memberId: Long,
    val status: String,
    val originalAmount: Long,
    val discountAmount: Long,
    val totalAmount: Long,
    val couponId: Long?,
    val orderedAt: ZonedDateTime,
    val items: List<OrderItemInfo>,
) {
    companion object {
        fun from(model: OrderModel): OrderInfo {
            return OrderInfo(
                id = model.id,
                orderNumber = model.orderNumber,
                memberId = model.memberId,
                status = model.status.name,
                originalAmount = model.getOriginalAmount(),
                discountAmount = model.discountAmount,
                totalAmount = model.getTotalAmount(),
                couponId = model.couponId,
                orderedAt = model.orderedAt,
                items = model.items.map { OrderItemInfo.from(it) },
            )
        }
    }
}

data class OrderItemInfo(
    val productId: Long,
    val productName: String,
    val productPrice: Long,
    val brandName: String,
    val quantity: Int,
    val amount: Long,
) {
    companion object {
        fun from(model: OrderItemModel): OrderItemInfo {
            return OrderItemInfo(
                productId = model.productId,
                productName = model.productName,
                productPrice = model.productPrice,
                brandName = model.brandName,
                quantity = model.quantity,
                amount = model.amount,
            )
        }
    }
}
