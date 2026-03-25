package com.loopers.domain.productlike.event

import org.springframework.context.ApplicationEvent
import java.util.UUID

class LikeCountEvent(
    source: Any,
    val productId: Long,
    val type: LikeCountEventType,
    val dedupeKey: String = "like.count:$productId:${type.name}:${UUID.randomUUID()}",
) : ApplicationEvent(source)

enum class LikeCountEventType {
    INCREMENT,
    DECREMENT,
}
