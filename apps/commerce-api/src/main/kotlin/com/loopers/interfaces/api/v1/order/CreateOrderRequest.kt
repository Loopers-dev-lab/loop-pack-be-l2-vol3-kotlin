package com.loopers.interfaces.api.v1.order

import com.loopers.application.order.CreateOrderCommand
import com.loopers.application.order.OrderItemCommand
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull

data class CreateOrderRequest(
    @field:NotEmpty(message = "주문 항목은 최소 1개 이상이어야 합니다.")
    @field:Valid
    val items: List<OrderItemRequest>,

    val couponId: Long? = null,
) {
    fun toCommand() = CreateOrderCommand(
        items = items.map { OrderItemCommand(productId = it.productId, quantity = it.quantity) },
        couponId = couponId,
    )
}

data class OrderItemRequest(
    @field:NotNull(message = "상품 ID는 필수입니다.")
    val productId: Long,

    @field:NotNull(message = "수량은 필수입니다.")
    @field:Min(value = 1, message = "수량은 1 이상이어야 합니다.")
    val quantity: Int,
)
