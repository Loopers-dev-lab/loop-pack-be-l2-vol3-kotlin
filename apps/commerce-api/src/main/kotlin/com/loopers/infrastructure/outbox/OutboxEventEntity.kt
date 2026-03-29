package com.loopers.infrastructure.outbox

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.ZonedDateTime

@Entity
@Table(
    name = "outbox_events",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_outbox_events_event_id", columnNames = ["event_id"]),
    ],
    indexes = [
        Index(name = "idx_outbox_events_published_at_id", columnList = "published_at, id"),
        Index(name = "idx_outbox_events_topic_key", columnList = "topic, message_key"),
    ],
)
class OutboxEventEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "event_id", nullable = false, length = 120)
    val eventId: String,

    @Column(name = "topic", nullable = false, length = 120)
    val topic: String,

    @Column(name = "message_key", nullable = false, length = 120)
    val messageKey: String,

    @Column(name = "event_type", nullable = false, length = 120)
    val eventType: String,

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    val payload: String,

    @Column(name = "attempt_count", nullable = false)
    var attemptCount: Int = 0,

    @Column(name = "published_at")
    var publishedAt: ZonedDateTime? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: ZonedDateTime? = null,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: ZonedDateTime? = null,
) {
    @PrePersist
    fun prePersist() {
        val now = ZonedDateTime.now()
        createdAt = now
        updatedAt = now
    }

    @PreUpdate
    fun preUpdate() {
        updatedAt = ZonedDateTime.now()
    }
}
