package com.loopers.domain.common.command

data class CreateOrderCommand(
    val memberId: Long,
    val items: List<CreateOrderItem>,
    val couponId: Long?,
) {
    data class CreateOrderItem(
        val productId: Long,
        val quantity: Int,
    )
}

data class CompleteOrderCommand(
    val orderId: Long,
)

data class CancelOrderCommand(
    val orderId: Long,
)
