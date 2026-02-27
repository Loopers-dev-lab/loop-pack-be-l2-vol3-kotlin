package com.loopers.interfaces.api.order

import com.loopers.application.order.GetOrderDetailResult
import com.loopers.application.order.GetOrderResult
import com.loopers.application.order.ListOrdersResult
import com.loopers.application.order.OrderItemResult
import com.loopers.domain.order.OrderStatus
import java.math.BigDecimal
import java.time.ZonedDateTime

class OrderV1AdminDto {
    data class OrderResponse(
        val id: Long,
        val userId: Long,
        val username: String,
        val status: OrderStatus,
        val totalPrice: BigDecimal,
        val createdAt: ZonedDateTime,
    ) {
        companion object {
            fun from(result: GetOrderResult): OrderResponse {
                return OrderResponse(
                    id = result.id,
                    userId = result.userId,
                    username = result.username,
                    status = result.status,
                    totalPrice = result.totalPrice,
                    createdAt = result.createdAt,
                )
            }
        }
    }

    data class OrderDetailResponse(
        val id: Long,
        val userId: Long,
        val username: String,
        val status: OrderStatus,
        val totalPrice: BigDecimal,
        val orderItems: List<OrderItemResponse>,
        val createdAt: ZonedDateTime,
    ) {
        companion object {
            fun from(result: GetOrderDetailResult): OrderDetailResponse {
                return OrderDetailResponse(
                    id = result.id,
                    userId = result.userId,
                    username = result.username,
                    status = result.status,
                    totalPrice = result.totalPrice,
                    orderItems = result.items.map { OrderItemResponse.from(it) },
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
            fun from(result: OrderItemResult): OrderItemResponse {
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
            fun from(result: ListOrdersResult): OrderSliceResponse {
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
