package com.loopers.domain.product.event

import com.loopers.domain.event.DomainEvent
import com.loopers.domain.event.EventType

/**
 * 상품 도메인 이벤트.
 *
 * 상품 조회 시 Kafka로 발행하여 product_metrics의 viewCount를 집계한다.
 */
object ProductEvent {

    data class Viewed(
        override val aggregateId: Long, // productId
        val userId: Long,
    ) : DomainEvent() {
        override val eventType: EventType = EventType.PRODUCT_VIEWED
    }
}
