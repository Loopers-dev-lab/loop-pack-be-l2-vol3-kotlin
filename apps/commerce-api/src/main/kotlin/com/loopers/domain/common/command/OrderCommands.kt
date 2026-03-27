package com.loopers.domain.common.command

data class CreateOrderCommand(
    val memberId: Long,
    val items: List<CreateOrderItem>,
    val couponId: Long?,
    val discountAmount: Long,
) {
    data class CreateOrderItem(
        val productId: Long,
        val productName: String,
        val productPrice: Long,
        val brandName: String,
        val quantity: Int,
    )
}

data class CompleteOrderCommand(
    val orderId: Long,
)

data class CancelOrderCommand(
    val orderId: Long,
)
