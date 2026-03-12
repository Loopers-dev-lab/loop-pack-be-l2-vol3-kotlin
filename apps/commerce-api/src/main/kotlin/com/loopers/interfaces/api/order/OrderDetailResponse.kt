package com.loopers.interfaces.api.order

import com.loopers.application.order.OrderDetailInfo
import com.loopers.application.order.OrderItemInfo
import java.time.ZonedDateTime

data class OrderDetailResponse(
    val orderId: Long,
    val originalAmount: Long,
    val discountAmount: Long,
    val totalAmount: Long,
    val status: String,
    val orderedAt: ZonedDateTime,
    val items: List<OrderDetailItemResponse>,
) {
    companion object {
        fun from(info: OrderDetailInfo): OrderDetailResponse {
            return OrderDetailResponse(
                orderId = info.orderId,
                originalAmount = info.originalAmount,
                discountAmount = info.discountAmount,
                totalAmount = info.totalAmount,
                status = info.status,
                orderedAt = info.orderedAt,
                items = info.items.map { OrderDetailItemResponse.from(it) },
            )
        }
    }
}

data class OrderDetailItemResponse(
    val productName: String,
    val productPrice: Long,
    val brandName: String,
    val imageUrl: String,
    val quantity: Int,
) {
    companion object {
        fun from(info: OrderItemInfo): OrderDetailItemResponse {
            return OrderDetailItemResponse(
                productName = info.productName,
                productPrice = info.productPrice,
                brandName = info.brandName,
                imageUrl = info.imageUrl,
                quantity = info.quantity,
            )
        }
    }
}
