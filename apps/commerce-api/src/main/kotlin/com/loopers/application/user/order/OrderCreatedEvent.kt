package com.loopers.application.user.order

import org.springframework.context.ApplicationEvent

class OrderCreatedEvent(
    val orderId: Long,
    val userId: Long,
    val productIds: List<Long>,
) : ApplicationEvent(orderId)
