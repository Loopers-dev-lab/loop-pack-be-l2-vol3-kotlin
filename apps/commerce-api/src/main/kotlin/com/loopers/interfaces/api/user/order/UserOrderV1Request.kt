package com.loopers.interfaces.api.user.order

import com.loopers.application.user.order.OrderCreateCommand
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull

class UserOrderV1Request {

    data class Create(
        @field:Valid
        @field:NotEmpty
        val items: List<Item>,
        val issuedCouponId: Long? = null,
    ) {
        data class Item(
            @field:NotNull
            val productId: Long,
            @field:Min(1)
            val quantity: Int,
        )

        fun toCommand(userId: Long, idempotencyKey: String): OrderCreateCommand =
            OrderCreateCommand(
                userId = userId,
                idempotencyKey = idempotencyKey,
                items = items.map { OrderCreateCommand.Item(it.productId, it.quantity) },
                issuedCouponId = issuedCouponId,
            )
    }
}
