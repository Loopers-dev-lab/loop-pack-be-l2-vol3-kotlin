package com.loopers.domain.like.event

import com.loopers.domain.event.DomainEvent
import com.loopers.domain.event.EventType

/**
 * 좋아요 도메인 이벤트.
 *
 * 좋아요/취소 시 likeCount 업데이트를 AFTER_COMMIT 이벤트로 분리한다.
 * → 집계 실패와 무관하게 좋아요 자체는 성공 (eventual consistency)
 */
object LikeEvent {

    data class Liked(
        override val aggregateId: Long, // productId
        val userId: Long,
    ) : DomainEvent() {
        override val eventType: EventType = EventType.PRODUCT_LIKED
    }

    data class Unliked(
        override val aggregateId: Long, // productId
        val userId: Long,
    ) : DomainEvent() {
        override val eventType: EventType = EventType.PRODUCT_UNLIKED
    }
}
