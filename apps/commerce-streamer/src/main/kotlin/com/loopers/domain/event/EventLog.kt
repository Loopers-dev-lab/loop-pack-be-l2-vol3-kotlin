package com.loopers.domain.event

import com.loopers.event.EventEnvelope
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

/**
 * 이벤트 로그 테이블: 감사/추적/디버깅용 (전체 이벤트 내용 보존).
 * payload, 실패 사유 등 상세 정보를 장기 보관한다.
 */
@Entity
@Table(name = "event_log")
class EventLog(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val eventId: String,

    @Column(nullable = false)
    val eventType: String,

    @Column(nullable = false)
    val aggregateType: String,

    @Column(nullable = false)
    val aggregateId: String,

    @Column(columnDefinition = "TEXT", nullable = false)
    val payload: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val status: EventLogStatus,

    val failReason: String? = null,

    @Column(nullable = false)
    val processedAt: Instant = Instant.now(),
) {
    companion object {
        fun success(envelope: EventEnvelope, aggregateType: String): EventLog = EventLog(
            eventId = envelope.eventId,
            eventType = envelope.eventType,
            aggregateType = aggregateType,
            aggregateId = envelope.aggregateId,
            payload = envelope.payload,
            status = EventLogStatus.SUCCESS,
        )
    }
}
