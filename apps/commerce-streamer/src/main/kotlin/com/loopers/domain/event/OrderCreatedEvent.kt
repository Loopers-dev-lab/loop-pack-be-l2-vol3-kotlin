package com.loopers.domain.event

import java.util.UUID

data class OrderCreatedEvent(
    val orderId: Long,
    val lineItems: List<OrderLineItem>,
    val dedupeKey: String = "order:$orderId:${UUID.randomUUID()}",
)
