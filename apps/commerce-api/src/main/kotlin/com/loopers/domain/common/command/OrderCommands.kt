package com.loopers.domain.common.command

data class CreateOrderCommand(
    val memberId: Long,
    val items: List<CreateOrderItem>,
    val couponId: Long?,
    val discountAmount: Long,
    val orderAmount: Long,
    val finalAmount: Long,
) {
    data class CreateOrderItem(
        val productId: Long,
        val quantity: Int,
        val productName: String,
        val productPrice: Long,
        val brandName: String,
    )
}

data class CompleteOrderCommand(
    val orderId: Long,
)

data class CancelOrderCommand(
    val orderId: Long,
)
