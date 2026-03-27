package com.loopers.config.kafka.event

import java.time.ZonedDateTime

data class CatalogEventMessage(
    val eventId: String,
    val productId: Long,
    val eventType: CatalogEventType,
    val delta: Long,
    val version: Long,
    val occurredAt: ZonedDateTime,
)

enum class CatalogEventType {
    LIKE_CHANGED,
    PRODUCT_VIEWED,
}
