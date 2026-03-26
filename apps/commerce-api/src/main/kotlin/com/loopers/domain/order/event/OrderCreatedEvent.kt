package com.loopers.domain.order.event

import org.springframework.context.ApplicationEvent

class OrderCreatedEvent(
    source: Any,
    val orderId: Long,
    val userId: Long,
    val itemCount: Int,
    val couponId: Long?,
    val dedupeKey: String,
) : ApplicationEvent(source)
