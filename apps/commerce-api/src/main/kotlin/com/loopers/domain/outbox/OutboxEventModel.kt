package com.loopers.domain.outbox

import com.loopers.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Index
import jakarta.persistence.Table

@Entity
@Table(
    name = "outbox_events",
    indexes = [Index(name = "idx_outbox_status_created", columnList = "status, created_at")],
)
class OutboxEventModel(
    aggregateType: String,
    aggregateId: Long,
    eventType: String,
    payload: String,
) : BaseEntity() {
    @Column(name = "aggregate_type", nullable = false, length = 50)
    var aggregateType: String = aggregateType
        protected set

    @Column(name = "aggregate_id", nullable = false)
    var aggregateId: Long = aggregateId
        protected set

    @Column(name = "event_type", nullable = false, length = 50)
    var eventType: String = eventType
        protected set

    @Column(nullable = false, columnDefinition = "TEXT")
    var payload: String = payload
        protected set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: OutboxStatus = OutboxStatus.PENDING
        protected set

    fun markSent() {
        status = OutboxStatus.SENT
    }

    fun markFailed() {
        status = OutboxStatus.FAILED
    }
}
