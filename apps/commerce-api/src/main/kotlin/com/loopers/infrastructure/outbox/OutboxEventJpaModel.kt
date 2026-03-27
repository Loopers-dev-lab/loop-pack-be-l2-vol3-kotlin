package com.loopers.infrastructure.outbox

import com.loopers.domain.common.outbox.OutboxEvent
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.PrePersist
import jakarta.persistence.Table
import java.time.ZonedDateTime

@Entity
@Table(name = "outbox_event")
class OutboxEventJpaModel(
    id: String,
    aggregateType: String,
    aggregateId: String,
    eventType: String,
    payload: String,
    partitionKey: String,
    topic: String,
) {
    @Id
    @Column(name = "id", nullable = false, length = 36)
    var id: String = id
        protected set

    @Column(name = "aggregate_type", nullable = false, length = 50)
    var aggregateType: String = aggregateType
        protected set

    @Column(name = "aggregate_id", nullable = false, length = 100)
    var aggregateId: String = aggregateId
        protected set

    @Column(name = "event_type", nullable = false, length = 100)
    var eventType: String = eventType
        protected set

    @Column(name = "payload", nullable = false, columnDefinition = "JSON")
    var payload: String = payload
        protected set

    @Column(name = "partition_key", nullable = false, length = 100)
    var partitionKey: String = partitionKey
        protected set

    @Column(name = "topic", nullable = false, length = 100)
    var topic: String = topic
        protected set

    @Column(name = "created_at", nullable = false, updatable = false)
    lateinit var createdAt: ZonedDateTime
        protected set

    @Column(name = "published_at")
    var publishedAt: ZonedDateTime? = null
        protected set

    @PrePersist
    private fun prePersist() {
        createdAt = ZonedDateTime.now()
    }

    fun markPublished() {
        publishedAt = ZonedDateTime.now()
    }

    fun toModel(): OutboxEvent = OutboxEvent(
        id = id,
        aggregateType = aggregateType,
        aggregateId = aggregateId,
        eventType = eventType,
        payload = payload,
        partitionKey = partitionKey,
        topic = topic,
        createdAt = createdAt,
        publishedAt = publishedAt,
    )

    companion object {
        fun from(model: OutboxEvent): OutboxEventJpaModel =
            OutboxEventJpaModel(
                id = model.id,
                aggregateType = model.aggregateType,
                aggregateId = model.aggregateId,
                eventType = model.eventType,
                payload = model.payload,
                partitionKey = model.partitionKey,
                topic = model.topic,
            )
    }
}
