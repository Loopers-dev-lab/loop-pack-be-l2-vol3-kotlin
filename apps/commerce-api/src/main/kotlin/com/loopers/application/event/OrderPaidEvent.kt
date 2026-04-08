package com.loopers.application.event

import java.time.ZonedDateTime

data class OrderPaidEvent(
    val orderId: Long,
    val memberId: Long,
    val items: List<Item>,
    val occurredAt: ZonedDateTime,
) {
    data class Item(
        val productId: Long,
        val quantity: Long,
    )
}
