package com.loopers.domain.event

import com.loopers.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table

@Entity
@Table(
    name = "outbox_events",
    indexes = [
        Index(name = "idx_outbox_published", columnList = "published, created_at"),
    ],
)
class OutboxEvent(
    @Column(name = "aggregate_type", nullable = false)
    val aggregateType: String,

    @Column(name = "aggregate_id", nullable = false)
    val aggregateId: String,

    @Column(name = "event_type", nullable = false)
    val eventType: String,

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    val payload: String,

    @Column(name = "topic", nullable = false)
    val topic: String,
) : BaseEntity() {
    @Column(name = "published", nullable = false)
    var published: Boolean = false
        protected set

    fun markPublished() {
        published = true
    }
}
