package com.loopers.domain.event

import java.time.LocalDateTime

data class LikeCreatedEvent(
    val userId: Long,
    val productId: Long,
    val occurredAt: LocalDateTime = LocalDateTime.now(),
)

data class LikeCancelledEvent(
    val userId: Long,
    val productId: Long,
    val occurredAt: LocalDateTime = LocalDateTime.now(),
)
