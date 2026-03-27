package com.loopers.interfaces.api.order

import com.loopers.application.order.OrderInfo
import com.loopers.application.order.OrderItemInfo
import com.loopers.domain.order.OrderStatus
import java.time.ZonedDateTime

class OrderV1Dto {
    data class CreateRequest(
        val items: List<OrderItemDto>,
        val couponIssueId: Long? = null,
    )

    data class OrderItemDto(
        val productId: Long,
        val quantity: Int,
    )

    data class OrderResponse(
        val id: Long,
        val userId: Long,
        val orderStatus: OrderStatus,
        val couponIssueId: Long?,
        val originalTotalAmount: Long,
        val discountAmount: Long,
        val totalAmount: Long,
        val orderItems: List<OrderItemResponse>,
        val createdAt: ZonedDateTime?,
    ) {
        companion object {
            fun from(info: OrderInfo): OrderResponse {
                return OrderResponse(
                    id = info.id,
                    userId = info.userId,
                    orderStatus = info.orderStatus,
                    couponIssueId = info.couponIssueId,
                    originalTotalAmount = info.originalTotalAmount,
                    discountAmount = info.discountAmount,
                    totalAmount = info.totalAmount,
                    orderItems = info.orderItems.map { OrderItemResponse.from(it) },
                    createdAt = info.createdAt,
                )
            }
        }
    }

    data class OrderItemResponse(
        val id: Long,
        val productId: Long,
        val productName: String,
        val brandName: String,
        val price: Long,
        val quantity: Int,
        val subTotal: Long,
    ) {
        companion object {
            fun from(info: OrderItemInfo): OrderItemResponse {
                return OrderItemResponse(
                    id = info.id,
                    productId = info.productId,
                    productName = info.productName,
                    brandName = info.brandName,
                    price = info.price,
                    quantity = info.quantity,
                    subTotal = info.subTotal,
                )
            }
        }
    }
}
