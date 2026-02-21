package com.loopers.interfaces.api.order.dto

import com.loopers.domain.order.OrderCommand
import com.loopers.domain.order.OrderDetail
import com.loopers.domain.order.entity.Order
import com.loopers.domain.order.entity.OrderItem
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotEmpty
import java.math.BigDecimal
import java.time.ZonedDateTime

class OrderV1Dto {
    data class CreateOrderRequest(
        @field:NotEmpty(message = "주문 항목은 필수입니다.")
        @field:Valid
        val items: List<CreateOrderItemRequest>,
    ) {
        fun toCommand(): OrderCommand.CreateOrder {
            return OrderCommand.CreateOrder(
                items = items.map {
                    OrderCommand.CreateOrderItem(productId = it.productId, quantity = it.quantity)
                },
            )
        }
    }

    data class CreateOrderItemRequest(
        val productId: Long,
        @field:Min(value = 1, message = "수량은 1 이상이어야 합니다.")
        val quantity: Int,
    )

    data class OrderResponse(
        val id: Long,
        val status: Order.OrderStatus,
        val totalPrice: BigDecimal,
        val items: List<OrderItemResponse>,
        val createdAt: ZonedDateTime,
    ) {
        companion object {
            fun from(orderDetail: OrderDetail): OrderResponse {
                return OrderResponse(
                    id = orderDetail.order.id,
                    status = orderDetail.order.status,
                    totalPrice = orderDetail.order.totalPrice,
                    items = orderDetail.items.map { OrderItemResponse.from(it) },
                    createdAt = orderDetail.order.createdAt,
                )
            }
        }
    }

    data class OrderItemResponse(
        val productId: Long,
        val productName: String,
        val productPrice: BigDecimal,
        val quantity: Int,
    ) {
        companion object {
            fun from(item: OrderItem): OrderItemResponse {
                return OrderItemResponse(
                    productId = item.refProductId,
                    productName = item.productName,
                    productPrice = item.productPrice,
                    quantity = item.quantity,
                )
            }
        }
    }
}
