package com.loopers.config.kafka.message

import java.time.ZonedDateTime

data class ProductLikedMessage(
    val eventId: String,
    val userId: Long,
    val productId: Long,
    val occurredAt: ZonedDateTime,
)
