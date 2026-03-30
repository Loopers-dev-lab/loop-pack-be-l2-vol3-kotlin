package com.loopers.event

import java.time.Instant

/**
 * Kafka 이벤트 공통 엔벨로프.
 *
 * 모든 이벤트는 이 포맷으로 직렬화되어 Kafka로 발행된다.
 * - eventId: 멱등성 키 (UUID)
 * - eventType: LIKED, UNLIKED, ORDER_COMPLETED 등
 * - aggregateId: productId, orderId 등 파티션 키로도 사용
 * - version: 최신성 판단용
 * - payload: 이벤트 타입별 상세 데이터 (JSON 문자열)
 */
data class EventEnvelope(
    val eventId: String,
    val eventType: String,
    val aggregateId: String,
    val version: Long,
    val timestamp: Instant,
    val payload: String,
)
