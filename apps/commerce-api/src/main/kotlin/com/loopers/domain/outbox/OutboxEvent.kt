package com.loopers.domain.outbox

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.PrePersist
import jakarta.persistence.Table
import java.time.ZonedDateTime

@Entity
@Table(
    name = "outbox_events",
    indexes = [
        Index(name = "idx_outbox_status_created", columnList = "status, created_at"),
    ],
)
class OutboxEvent(
    aggregateType: String,
    aggregateId: String,
    eventType: String,
    topic: String,
    partitionKey: String,
    payload: String,
) {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0

    @Column(name = "aggregate_type", nullable = false)
    val aggregateType: String = aggregateType

    @Column(name = "aggregate_id", nullable = false)
    val aggregateId: String = aggregateId

    @Column(name = "event_type", nullable = false)
    val eventType: String = eventType

    @Column(name = "topic", nullable = false)
    val topic: String = topic

    @Column(name = "partition_key", nullable = false)
    val partitionKey: String = partitionKey

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    val payload: String = payload

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: OutboxStatus = OutboxStatus.PENDING
        private set

    @Column(name = "created_at", nullable = false, updatable = false)
    lateinit var createdAt: ZonedDateTime
        private set

    @Column(name = "sent_at")
    var sentAt: ZonedDateTime? = null
        private set

    @PrePersist
    private fun prePersist() {
        createdAt = ZonedDateTime.now()
    }

    fun markSent() {
        status = OutboxStatus.SENT
        sentAt = ZonedDateTime.now()
    }

    fun markFailed() {
        status = OutboxStatus.FAILED
    }
}

enum class OutboxStatus {
    PENDING,
    SENT,
    FAILED,
}
