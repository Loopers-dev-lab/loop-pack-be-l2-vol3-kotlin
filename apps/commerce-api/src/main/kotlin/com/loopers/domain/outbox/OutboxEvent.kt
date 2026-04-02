package com.loopers.domain.outbox

import com.loopers.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.time.ZonedDateTime
import java.util.UUID

@Entity
@Table(name = "outbox_events")
class OutboxEvent private constructor(
    eventId: String,
    eventType: String,
    topic: String,
    partitionKey: String,
    payload: String,
) : BaseEntity() {

    @Column(name = "event_id", nullable = false, unique = true, length = 36)
    val eventId: String = eventId

    @Column(name = "event_type", nullable = false, length = 100)
    val eventType: String = eventType

    @Column(name = "topic", nullable = false, length = 100)
    val topic: String = topic

    @Column(name = "partition_key", nullable = false, length = 100)
    val partitionKey: String = partitionKey

    @Column(name = "payload", nullable = false, columnDefinition = "JSON")
    val payload: String = payload

    @Column(name = "published", nullable = false)
    var published: Boolean = false
        protected set

    @Column(name = "published_at")
    var publishedAt: ZonedDateTime? = null
        protected set

    fun markPublished() {
        this.published = true
        this.publishedAt = ZonedDateTime.now()
    }

    companion object {
        fun create(
            eventType: String,
            topic: String,
            partitionKey: String,
            payload: String,
        ): OutboxEvent {
            return OutboxEvent(
                eventId = UUID.randomUUID().toString(),
                eventType = eventType,
                topic = topic,
                partitionKey = partitionKey,
                payload = payload,
            )
        }
    }
}
