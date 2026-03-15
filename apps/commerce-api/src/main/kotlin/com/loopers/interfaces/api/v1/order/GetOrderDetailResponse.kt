package com.loopers.interfaces.api.v1.order

import com.loopers.application.order.OrderInfo
import com.loopers.application.order.OrderItemInfo
import java.time.ZonedDateTime

data class GetOrderDetailResponse(
    val id: Long,
    val status: String,
    val userCouponId: Long?,
    val originalAmount: Long,
    val discountAmount: Long,
    val totalAmount: Long,
    val orderedAt: ZonedDateTime,
    val items: List<OrderItemResponse>,
) {
    companion object {
        fun from(orderInfo: OrderInfo) = GetOrderDetailResponse(
            id = orderInfo.id,
            status = orderInfo.status,
            userCouponId = orderInfo.userCouponId,
            originalAmount = orderInfo.originalAmount,
            discountAmount = orderInfo.discountAmount,
            totalAmount = orderInfo.totalAmount,
            orderedAt = orderInfo.orderedAt,
            items = orderInfo.items.map { OrderItemResponse.from(it) },
        )
    }
}

data class OrderItemResponse(
    val id: Long,
    val productId: Long,
    val productName: String,
    val brandName: String,
    val price: Long,
    val quantity: Int,
) {
    companion object {
        fun from(itemInfo: OrderItemInfo) = OrderItemResponse(
            id = itemInfo.id,
            productId = itemInfo.productId,
            productName = itemInfo.productName,
            brandName = itemInfo.brandName,
            price = itemInfo.price,
            quantity = itemInfo.quantity,
        )
    }
}
