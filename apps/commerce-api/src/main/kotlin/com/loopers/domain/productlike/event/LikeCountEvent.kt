package com.loopers.domain.productlike.event

import java.util.UUID

data class LikeCountEvent(
    val productId: Long,
    val type: LikeCountEventType,
    val userId: Long? = null,
    val dedupeKey: String = "like.count:$productId:${type.name}:${UUID.randomUUID()}",
)

enum class LikeCountEventType {
    INCREMENT,
    DECREMENT,
}
