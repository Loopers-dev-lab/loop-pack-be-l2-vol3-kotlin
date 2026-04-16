package com.loopers.application.event

import java.time.ZonedDateTime

data class ProductLikeChangedEvent(
    val productId: Long,
    val brandId: Long,
    val delta: Long,
    val occurredAt: ZonedDateTime,
)
