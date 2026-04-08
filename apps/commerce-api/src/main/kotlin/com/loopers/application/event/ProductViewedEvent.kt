package com.loopers.application.event

import java.time.ZonedDateTime

data class ProductViewedEvent(
    val productId: Long,
    val occurredAt: ZonedDateTime,
)
