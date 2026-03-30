package com.loopers.event.payload

data class OrderCompletedPayload(
    val orderId: Long,
    val userId: Long,
    val items: List<OrderItemPayload>,
    val couponId: Long?,
    val totalAmount: Long,
    val paymentAmount: Long,
)

data class OrderItemPayload(
    val productId: Long,
    val quantity: Int,
    val productName: String,
)
