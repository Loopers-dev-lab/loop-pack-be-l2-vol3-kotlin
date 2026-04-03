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
    val retryCount: Int,
    val occurredAt: ZonedDateTime,
) {
    fun markPublished(): OutboxEvent = copy(status = OutboxEventStatus.PUBLISHED)

    fun markFailed(): OutboxEvent = copy(
        status = OutboxEventStatus.FAILED,
        retryCount = retryCount + 1,
    )

    fun markDead(): OutboxEvent = copy(status = OutboxEventStatus.DEAD)

    fun isRetryExhausted(maxRetry: Int): Boolean = retryCount >= maxRetry

    private fun copy(
        status: OutboxEventStatus = this.status,
        retryCount: Int = this.retryCount,
    ): OutboxEvent = OutboxEvent(
        persistenceId = persistenceId,
        eventId = eventId,
        eventType = eventType,
        aggregateId = aggregateId,
        topic = topic,
        partitionKey = partitionKey,
        payload = payload,
        status = status,
        retryCount = retryCount,
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
            retryCount = 0,
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
            retryCount: Int,
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
            retryCount = retryCount,
            occurredAt = occurredAt,
        )
    }
}
