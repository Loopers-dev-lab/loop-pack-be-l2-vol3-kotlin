package com.loopers.application.event

import java.time.ZonedDateTime
import java.util.UUID

data class OrderCreatedEvent(
    val eventId: String = UUID.randomUUID().toString(),
    val orderId: Long,
    val userId: Long,
    val totalAmount: Long,
    val itemCount: Int,
    val occurredAt: ZonedDateTime = ZonedDateTime.now(),
)

data class OrderCompletedEvent(
    val eventId: String = UUID.randomUUID().toString(),
    val orderId: Long,
    val userId: Long,
    val occurredAt: ZonedDateTime = ZonedDateTime.now(),
)

data class ProductLikedEvent(
    val eventId: String = UUID.randomUUID().toString(),
    val productId: Long,
    val userId: Long,
    val occurredAt: ZonedDateTime = ZonedDateTime.now(),
)

data class ProductUnlikedEvent(
    val eventId: String = UUID.randomUUID().toString(),
    val productId: Long,
    val userId: Long,
    val occurredAt: ZonedDateTime = ZonedDateTime.now(),
)

data class ProductViewedEvent(
    val eventId: String = UUID.randomUUID().toString(),
    val productId: Long,
    val occurredAt: ZonedDateTime = ZonedDateTime.now(),
)
