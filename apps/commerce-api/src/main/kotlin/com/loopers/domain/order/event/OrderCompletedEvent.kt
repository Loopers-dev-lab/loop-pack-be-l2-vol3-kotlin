package com.loopers.domain.order.event

data class OrderCompletedEvent(
    val orderId: Long,
    val userId: Long,
    val items: List<OrderCompletedItem>,
)

data class OrderCompletedItem(
    val productId: Long,
    val quantity: Int,
    val productName: String,
)
