package com.loopers.interfaces.api.admin.order

import com.loopers.application.order.OrderInfo
import com.loopers.application.order.OrderItemInfo

class AdminOrderV1Dto {
    data class OrderResponse(
        val id: Long,
        val userId: Long,
        val originalTotalPrice: Long,
        val discountAmount: Long,
        val totalPrice: Long,
        val couponId: Long?,
        val items: List<OrderItemResponse>,
    ) {
        companion object {
            fun from(orderInfo: OrderInfo): OrderResponse {
                return OrderResponse(
                    id = orderInfo.id,
                    userId = orderInfo.userId,
                    originalTotalPrice = orderInfo.originalTotalPrice,
                    discountAmount = orderInfo.discountAmount,
                    totalPrice = orderInfo.totalPrice,
                    couponId = orderInfo.couponId,
                    items = orderInfo.items.map { OrderItemResponse.from(it) },
                )
            }
        }
    }

    data class OrderItemResponse(
        val productId: Long,
        val productName: String,
        val productPrice: Long,
        val quantity: Int,
    ) {
        companion object {
            fun from(itemInfo: OrderItemInfo): OrderItemResponse {
                return OrderItemResponse(
                    productId = itemInfo.productId,
                    productName = itemInfo.productName,
                    productPrice = itemInfo.productPrice,
                    quantity = itemInfo.quantity,
                )
            }
        }
    }
}
