package com.loopers.domain.outbox

import com.loopers.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.ZonedDateTime

@Entity
@Table(
    name = "outbox_event",
    indexes = [
        Index(name = "idx_outbox_event_published_at_id", columnList = "published_at, id"),
        Index(name = "idx_outbox_event_partition_key", columnList = "partition_key"),
    ],
)
class OutboxEventModel(
    eventId: String,
    topic: String,
    partitionKey: String,
    payload: String,
) : BaseEntity() {
    @Column(name = "event_id", nullable = false, unique = true, length = 100)
    var eventId: String = eventId
        protected set

    @Column(name = "topic", nullable = false, length = 100)
    var topic: String = topic
        protected set

    @Column(name = "partition_key", nullable = false, length = 200)
    var partitionKey: String = partitionKey
        protected set

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    var payload: String = payload
        protected set

    @Column(name = "published_at")
    var publishedAt: ZonedDateTime? = null
        protected set

    fun markPublished(now: ZonedDateTime = ZonedDateTime.now()) {
        publishedAt = now
    }
}
