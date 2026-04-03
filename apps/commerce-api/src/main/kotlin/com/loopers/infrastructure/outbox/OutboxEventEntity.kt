package com.loopers.infrastructure.outbox

import com.loopers.domain.outbox.OutboxEventStatus
import com.loopers.support.jpa.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Index
import jakarta.persistence.Table

@Entity
@Table(
    name = "outbox_event",
    indexes = [
        Index(name = "idx_outbox_status_created", columnList = "status, created_at"),
    ],
)
class OutboxEventEntity(
    id: Long?,

    @Column(name = "event_id", nullable = false, unique = true, length = 36)
    val eventId: String,

    @Column(name = "event_type", nullable = false, length = 50)
    val eventType: String,

    @Column(name = "aggregate_id", nullable = false, length = 100)
    val aggregateId: String,

    @Column(name = "topic", nullable = false, length = 100)
    val topic: String,

    @Column(name = "partition_key", nullable = false, length = 100)
    val partitionKey: String,

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    val payload: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    val status: OutboxEventStatus,

    @Column(name = "retry_count", nullable = false)
    val retryCount: Int = 0,

    @Column(name = "occurred_at", nullable = false)
    val occurredAt: java.time.ZonedDateTime,
) : BaseEntity() {
    init {
        this.id = id
    }
}
