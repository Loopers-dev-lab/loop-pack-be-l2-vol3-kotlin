package com.loopers.interfaces.api.order

import com.loopers.application.order.OrderInfo
import com.loopers.application.order.OrderItemInfo
import com.loopers.domain.order.OrderStatus
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotEmpty
import java.time.ZonedDateTime

class OrderV1Dto {
    data class CreateRequest(
        @field:NotEmpty(message = "주문 항목은 최소 1개 이상이어야 합니다.")
        @field:Valid
        val items: List<CreateOrderItemRequest>,
    )

    data class CreateOrderItemRequest(
        val productId: Long,
        @field:Min(value = 1, message = "주문 수량은 1 이상이어야 합니다.")
        val quantity: Int,
    )

    data class OrderResponse(
        val orderId: Long,
        val orderNumber: String,
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
                    status = info.status,
                    totalAmount = info.totalAmount,
                    orderedAt = info.orderedAt,
                    items = info.items.map { OrderItemResponse.from(it) },
                )
            }
        }
    }

    data class OrderItemResponse(
        val productName: String,
        val brandName: String,
        val price: Long,
        val quantity: Int,
        val amount: Long,
    ) {
        companion object {
            fun from(info: OrderItemInfo): OrderItemResponse {
                return OrderItemResponse(
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
