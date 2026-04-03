package com.loopers.support.event.user

import org.springframework.context.ApplicationEvent

class OrderCreatedEvent(
    val orderId: Long,
    val userId: Long,
    val productIds: List<Long>,
    val hasEntryToken: Boolean = false,
) : ApplicationEvent(orderId)
