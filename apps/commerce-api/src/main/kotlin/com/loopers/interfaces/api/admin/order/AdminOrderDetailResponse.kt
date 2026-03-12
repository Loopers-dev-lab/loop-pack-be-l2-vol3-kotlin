package com.loopers.interfaces.api.admin.order

import com.loopers.application.order.OrderDetailInfo
import com.loopers.application.order.OrderItemInfo
import java.time.ZonedDateTime

data class AdminOrderDetailResponse(
    val orderId: Long,
    val userId: Long,
    val originalAmount: Long,
    val discountAmount: Long,
    val totalAmount: Long,
    val status: String,
    val orderedAt: ZonedDateTime,
    val items: List<AdminOrderDetailItemResponse>,
) {
    companion object {
        fun from(info: OrderDetailInfo): AdminOrderDetailResponse {
            return AdminOrderDetailResponse(
                orderId = info.orderId,
                userId = info.userId,
                originalAmount = info.originalAmount,
                discountAmount = info.discountAmount,
                totalAmount = info.totalAmount,
                status = info.status,
                orderedAt = info.orderedAt,
                items = info.items.map { AdminOrderDetailItemResponse.from(it) },
            )
        }
    }
}

data class AdminOrderDetailItemResponse(
    val productName: String,
    val productPrice: Long,
    val brandName: String,
    val imageUrl: String,
    val quantity: Int,
) {
    companion object {
        fun from(info: OrderItemInfo): AdminOrderDetailItemResponse {
            return AdminOrderDetailItemResponse(
                productName = info.productName,
                productPrice = info.productPrice,
                brandName = info.brandName,
                imageUrl = info.imageUrl,
                quantity = info.quantity,
            )
        }
    }
}
