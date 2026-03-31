package com.loopers.infrastructure.outbox

import com.loopers.infrastructure.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.ZonedDateTime

@Table(
    name = "event_outbox",
    indexes = [
        Index(name = "idx_event_outbox_published_id", columnList = "published_at, id"),
        Index(name = "idx_event_outbox_topic_key_id", columnList = "topic, event_key, id"),
    ],
)
@Entity
class KafkaOutboxEntity(
    id: Long? = null,
    @Column(name = "topic", nullable = false)
    val topic: String,
    @Column(name = "event_key", nullable = false)
    val eventKey: String,
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    val eventType: KafkaEventType,
    @Column(name = "aggregate_id", nullable = false)
    val aggregateId: Long,
    @Column(name = "payload", nullable = false, columnDefinition = "text")
    val payload: String,
    @Column(name = "published_at")
    var publishedAt: ZonedDateTime? = null,
) : BaseEntity() {
    init {
        this.id = id
    }

    fun markPublished(at: ZonedDateTime = ZonedDateTime.now()) {
        publishedAt = at
    }
}
