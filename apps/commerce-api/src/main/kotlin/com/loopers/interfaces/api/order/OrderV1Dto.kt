package com.loopers.interfaces.api.order

import com.loopers.application.order.OrderInfo
import java.time.ZonedDateTime

class OrderV1Dto {
    data class CreateOrderRequest(
        val items: List<OrderItemRequest>,
    ) {
        data class OrderItemRequest(
            val productId: Long,
            val quantity: Long,
        )
    }

    data class OrderResponse(
        val id: Long,
        val userId: Long,
        val totalAmount: Long,
        val status: String,
        val items: List<OrderItemResponse>,
        val createdAt: ZonedDateTime,
        val updatedAt: ZonedDateTime,
    ) {
        data class OrderItemResponse(
            val id: Long,
            val productId: Long,
            val brandId: Long,
            val productName: String,
            val imageUrl: String,
            val unitPrice: Long,
            val quantity: Long,
            val subTotal: Long,
        )

        companion object {
            fun from(info: OrderInfo) = OrderResponse(
                id = info.id,
                userId = info.userId,
                totalAmount = info.totalAmount,
                status = info.status,
                items = info.items.map { item ->
                    OrderItemResponse(
                        id = item.id,
                        productId = item.productId,
                        brandId = item.brandId,
                        productName = item.productName,
                        imageUrl = item.imageUrl,
                        unitPrice = item.unitPrice,
                        quantity = item.quantity,
                        subTotal = item.subTotal,
                    )
                },
                createdAt = info.createdAt,
                updatedAt = info.updatedAt,
            )
        }
    }
}
