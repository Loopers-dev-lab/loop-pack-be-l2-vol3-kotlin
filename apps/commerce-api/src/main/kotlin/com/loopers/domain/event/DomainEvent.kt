package com.loopers.domain.event

import java.time.ZonedDateTime
import java.util.UUID

/**
 * 모든 도메인 이벤트의 베이스.
 *
 * - eventId: 멱등 처리를 위한 고유 식별자 (UUID v4)
 * - occurredAt: 이벤트 발생 시점
 * - eventType: Kafka 토픽 라우팅 및 Outbox 분류에 사용
 *
 * JPA 엔티티가 아닌 순수 도메인 객체.
 * Domain 레이어에 위치하므로 인프라 기술(JPA 등)에 의존하지 않는다.
 */
abstract class DomainEvent(
    val eventId: String = UUID.randomUUID().toString(),
    val occurredAt: ZonedDateTime = ZonedDateTime.now(),
) {
    abstract val eventType: EventType
    abstract val aggregateId: Long
}
