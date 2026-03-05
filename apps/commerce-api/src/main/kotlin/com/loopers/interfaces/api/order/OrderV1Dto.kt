package com.loopers.interfaces.api.order

import com.loopers.application.order.UserGetOrderItemResult
import com.loopers.application.order.UserGetOrderResult
import com.loopers.application.order.UserGetOrdersResult
import com.loopers.domain.order.OrderStatus
import java.math.BigDecimal
import java.time.ZonedDateTime

class OrderV1Dto {
    data class CreateOrderRequest(
        val items: List<CreateOrderItemRequest>,
        val couponId: Long? = null,
    )

    data class CreateOrderItemRequest(
        val productId: Long,
        val quantity: Int,
    )

    data class OrderResponse(
        val id: Long,
        val status: OrderStatus,
        val originalPrice: BigDecimal,
        val discountAmount: BigDecimal,
        val totalPrice: BigDecimal,
        val items: List<OrderItemResponse>,
        val createdAt: ZonedDateTime,
    ) {
        companion object {
            fun from(result: UserGetOrderResult): OrderResponse {
                return OrderResponse(
                    id = result.id,
                    status = result.status,
                    originalPrice = result.originalPrice,
                    discountAmount = result.discountAmount,
                    totalPrice = result.totalPrice,
                    items = result.items.map { OrderItemResponse.from(it) },
                    createdAt = result.createdAt,
                )
            }
        }
    }

    data class OrderItemResponse(
        val id: Long,
        val productId: Long,
        val productName: String,
        val quantity: Int,
        val price: BigDecimal,
    ) {
        companion object {
            fun from(result: UserGetOrderItemResult): OrderItemResponse {
                return OrderItemResponse(
                    id = result.id,
                    productId = result.productId,
                    productName = result.productName,
                    quantity = result.quantity,
                    price = result.price,
                )
            }
        }
    }

    data class OrderSliceResponse(
        val content: List<OrderResponse>,
        val page: Int,
        val size: Int,
        val hasNext: Boolean,
    ) {
        companion object {
            fun from(result: UserGetOrdersResult): OrderSliceResponse {
                return OrderSliceResponse(
                    content = result.content.map { OrderResponse.from(it) },
                    page = result.page,
                    size = result.size,
                    hasNext = result.hasNext,
                )
            }
        }
    }
}
