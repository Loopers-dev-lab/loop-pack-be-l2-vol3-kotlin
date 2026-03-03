package com.loopers.interfaces.api.order.dto

import com.loopers.application.order.OrderInfo
import com.loopers.application.order.OrderItemInfo
import com.loopers.application.order.PlaceOrderCommand
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotEmpty
import java.math.BigDecimal

class OrderV1Dto {

    data class CreateOrderRequest(
        @field:NotEmpty(message = "주문 항목은 필수입니다.")
        @field:Valid
        val items: List<CreateOrderItemRequest>,
        val couponId: Long? = null,
    ) {
        fun toCommand(): PlaceOrderCommand {
            return PlaceOrderCommand(
                items = items.map {
                    PlaceOrderCommand.PlaceOrderItemCommand(productId = it.productId, quantity = it.quantity)
                },
                couponId = couponId,
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
        val status: String,
        val originalPrice: BigDecimal,
        val discountAmount: BigDecimal,
        val totalPrice: BigDecimal,
        val couponId: Long?,
        val items: List<OrderItemResponse>,
    ) {
        companion object {
            fun from(info: OrderInfo): OrderResponse {
                return OrderResponse(
                    id = info.id,
                    status = info.status,
                    originalPrice = info.originalPrice,
                    discountAmount = info.discountAmount,
                    totalPrice = info.totalPrice,
                    couponId = info.couponId,
                    items = info.items.map { OrderItemResponse.from(it) },
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
            fun from(item: OrderItemInfo): OrderItemResponse {
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
