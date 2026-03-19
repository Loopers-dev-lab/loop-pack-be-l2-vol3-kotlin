package com.loopers.domain.productlike.event

import org.springframework.context.ApplicationEvent

class LikeCountEvent(
    source: Any,
    val productId: Long,
    val type: LikeCountEventType,
) : ApplicationEvent(source)

enum class LikeCountEventType {
    INCREMENT,
    DECREMENT,
}
