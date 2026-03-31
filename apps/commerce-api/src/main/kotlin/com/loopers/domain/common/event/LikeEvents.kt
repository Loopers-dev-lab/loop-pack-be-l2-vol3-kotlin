package com.loopers.domain.common.event

data class LikeCreatedEvent(
    val memberId: Long,
    val productId: Long,
)

data class LikeCancelledEvent(
    val memberId: Long,
    val productId: Long,
)
