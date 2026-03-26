package com.loopers.domain.order.event

import org.springframework.context.ApplicationEvent
import java.util.UUID

data class OrderLineItem(
    val productId: Long,
    val quantity: Int,
)

class OrderCreatedEvent(
    source: Any,
    val orderId: Long,
    val lineItems: List<OrderLineItem>,
    val dedupeKey: String = "order:$orderId:${UUID.randomUUID()}",
) : ApplicationEvent(source)
