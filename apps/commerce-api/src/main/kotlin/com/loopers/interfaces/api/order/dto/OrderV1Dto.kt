package com.loopers.interfaces.api.order.dto

import com.loopers.application.order.OrderInfo
import com.loopers.application.order.PlaceOrderCommand
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotEmpty
import java.math.BigDecimal
import java.time.ZonedDateTime

class OrderV1Dto {

    enum class OrderStatusDto { CREATED, PAID, CANCELLED, FAILED }
    enum class OrderItemStatusDto { ACTIVE, CANCELLED }

    data class CreateOrderRequest(
        @field:NotEmpty(message = "주문 항목은 필수입니다.")
        @field:Valid
        val items: List<CreateOrderItemRequest>,
    ) {
        fun toCommand(): PlaceOrderCommand {
            return PlaceOrderCommand(
                items = items.map {
                    PlaceOrderCommand.PlaceOrderItemCommand(productId = it.productId, quantity = it.quantity)
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
        val status: OrderStatusDto,
        val totalPrice: BigDecimal,
        val items: List<OrderItemResponse>,
        val createdAt: ZonedDateTime,
    ) {
        companion object {
            fun from(info: OrderInfo): OrderResponse {
                return OrderResponse(
                    id = info.id,
                    status = OrderStatusDto.valueOf(info.status),
                    totalPrice = info.totalPrice,
                    items = info.items.map { OrderItemResponse.from(it) },
                    createdAt = info.createdAt,
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
            fun from(item: com.loopers.application.order.OrderItemInfo): OrderItemResponse {
                return OrderItemResponse(
                    productId = item.productId,
                    productName = item.productName,
                    productPrice = item.productPrice,
                    quantity = item.quantity,
                )
            }
        }
    }
}
