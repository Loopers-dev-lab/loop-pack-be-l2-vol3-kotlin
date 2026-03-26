package com.loopers.domain.outbox

import java.time.ZonedDateTime

class OutboxEvent private constructor(
    val persistenceId: Long?,
    val eventId: String,
    val eventType: String,
    val aggregateId: String,
    val topic: String,
    val partitionKey: String,
    val payload: String,
    val status: OutboxEventStatus,
    val occurredAt: ZonedDateTime,
) {
    fun markPublished(): OutboxEvent = OutboxEvent(
        persistenceId = persistenceId,
        eventId = eventId,
        eventType = eventType,
        aggregateId = aggregateId,
        topic = topic,
        partitionKey = partitionKey,
        payload = payload,
        status = OutboxEventStatus.PUBLISHED,
        occurredAt = occurredAt,
    )

    fun markFailed(): OutboxEvent = OutboxEvent(
        persistenceId = persistenceId,
        eventId = eventId,
        eventType = eventType,
        aggregateId = aggregateId,
        topic = topic,
        partitionKey = partitionKey,
        payload = payload,
        status = OutboxEventStatus.FAILED,
        occurredAt = occurredAt,
    )

    companion object {
        fun create(
            eventId: String,
            eventType: String,
            aggregateId: String,
            topic: String,
            partitionKey: String,
            payload: String,
            occurredAt: ZonedDateTime,
        ): OutboxEvent = OutboxEvent(
            persistenceId = null,
            eventId = eventId,
            eventType = eventType,
            aggregateId = aggregateId,
            topic = topic,
            partitionKey = partitionKey,
            payload = payload,
            status = OutboxEventStatus.PENDING,
            occurredAt = occurredAt,
        )

        fun reconstitute(
            persistenceId: Long,
            eventId: String,
            eventType: String,
            aggregateId: String,
            topic: String,
            partitionKey: String,
            payload: String,
            status: OutboxEventStatus,
            occurredAt: ZonedDateTime,
        ): OutboxEvent = OutboxEvent(
            persistenceId = persistenceId,
            eventId = eventId,
            eventType = eventType,
            aggregateId = aggregateId,
            topic = topic,
            partitionKey = partitionKey,
            payload = payload,
            status = status,
            occurredAt = occurredAt,
        )
    }
}
