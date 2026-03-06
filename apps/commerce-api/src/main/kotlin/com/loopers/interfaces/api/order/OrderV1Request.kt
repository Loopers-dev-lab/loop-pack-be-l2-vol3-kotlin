package com.loopers.interfaces.api.order

import com.loopers.application.order.OrderCommand
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull

data class OrderCreateRequest(
    @field:NotEmpty(message = "주문 항목은 비어있을 수 없습니다.")
    @field:Valid
    val items: List<OrderItemRequest>,
    val userCouponId: Long? = null,
) {
    fun toCommand(userId: Long): OrderCommand.Create {
        return OrderCommand.Create(
            userId = userId,
            items = items.map {
                OrderCommand.Create.OrderLineItem(
                    productId = requireNotNull(it.productId),
                    quantity = requireNotNull(it.quantity),
                )
            },
            userCouponId = userCouponId,
        )
    }
}

data class OrderItemRequest(
    @field:NotNull(message = "상품 ID는 필수입니다.")
    val productId: Long?,

    @field:NotNull(message = "수량은 필수입니다.")
    @field:Min(value = 1, message = "수량은 1 이상이어야 합니다.")
    val quantity: Int?,
)
