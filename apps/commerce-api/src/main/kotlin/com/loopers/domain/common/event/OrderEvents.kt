package com.loopers.domain.common.event

data class OrderRequestedEvent(
    val memberId: Long,
    val items: List<OrderRequestedItem>,
    val couponId: Long?,
) {
    data class OrderRequestedItem(
        val productId: Long,
        val quantity: Int,
    )
}

data class OrderCreatedEvent(
    val orderId: Long,
    val memberId: Long,
    val orderNumber: String,
)

data class OrderCancelledEvent(
    val orderId: Long,
    val memberId: Long,
)

data class OrderPaidEvent(
    val orderId: Long,
    val memberId: Long,
)
