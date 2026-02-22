package com.loopers.interfaces.api.admin.order

import com.loopers.application.order.OrderInfo
import com.loopers.application.order.OrderItemInfo
import com.loopers.domain.order.OrderStatus
import java.time.ZonedDateTime

class AdminOrderV1Dto {
    data class OrderResponse(
        val orderId: Long,
        val orderNumber: String,
        val memberId: Long,
        val status: OrderStatus,
        val totalAmount: Long,
        val orderedAt: ZonedDateTime,
        val items: List<OrderItemResponse>,
    ) {
        companion object {
            fun from(info: OrderInfo): OrderResponse {
                return OrderResponse(
                    orderId = info.id,
                    orderNumber = info.orderNumber,
                    memberId = info.memberId,
                    status = info.status,
                    totalAmount = info.totalAmount,
                    orderedAt = info.orderedAt,
                    items = info.items.map { OrderItemResponse.from(it) },
                )
            }
        }
    }

    data class OrderItemResponse(
        val productId: Long,
        val productName: String,
        val brandName: String,
        val price: Long,
        val quantity: Int,
        val amount: Long,
    ) {
        companion object {
            fun from(info: OrderItemInfo): OrderItemResponse {
                return OrderItemResponse(
                    productId = info.productId,
                    productName = info.productName,
                    brandName = info.brandName,
                    price = info.productPrice,
                    quantity = info.quantity,
                    amount = info.amount,
                )
            }
        }
    }
}
