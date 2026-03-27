package com.loopers.domain.outbox

import com.loopers.domain.BaseEntity
import com.loopers.domain.event.EventType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import org.hibernate.annotations.Comment

/**
 * Transactional Outbox 패턴용 이벤트 저장 엔티티.
 *
 * 비즈니스 트랜잭션과 같은 TX에서 저장되어 원자성을 보장한다.
 * 별도 릴레이(스케줄러)가 PENDING 상태인 이벤트를 Kafka로 발행한 후,
 * 헬스체크 컨슈머가 PUBLISHED로 마킹한다.
 */
@Entity
@Table(name = "outbox_event")
@Comment("Outbox 이벤트")
class OutboxEvent(
    eventId: String,
    eventType: EventType,
    aggregateId: Long,
    topic: String,
    partitionKey: String,
    payload: String,
) : BaseEntity() {

    @Comment("도메인 이벤트 고유 ID (UUID)")
    @Column(name = "event_id", nullable = false, unique = true)
    var eventId: String = eventId
        protected set

    @Comment("이벤트 타입")
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    var eventType: EventType = eventType
        protected set

    @Comment("집계 대상 ID (주문ID, 상품ID 등)")
    @Column(name = "aggregate_id", nullable = false)
    var aggregateId: Long = aggregateId
        protected set

    @Comment("Kafka 토픽명")
    @Column(name = "topic", nullable = false)
    var topic: String = topic
        protected set

    @Comment("Kafka 파티션 키")
    @Column(name = "partition_key", nullable = false)
    var partitionKey: String = partitionKey
        protected set

    @Comment("이벤트 페이로드 (JSON)")
    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    var payload: String = payload
        protected set

    @Comment("발행 상태")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: OutboxStatus = OutboxStatus.PENDING
        protected set

    fun markPublished() {
        this.status = OutboxStatus.PUBLISHED
    }

    fun markFailed() {
        this.status = OutboxStatus.FAILED
    }
}
