package com.loopers.infrastructure.outbox

import com.loopers.domain.outbox.OutboxEvent

object OutboxEventMapper {
    fun toDomain(entity: OutboxEventEntity): OutboxEvent {
        val id = requireNotNull(entity.id) { "OutboxEventEntity.id가 null입니다." }
        return OutboxEvent.reconstitute(
            persistenceId = id,
            eventId = entity.eventId,
            eventType = entity.eventType,
            aggregateId = entity.aggregateId,
            topic = entity.topic,
            partitionKey = entity.partitionKey,
            payload = entity.payload,
            status = entity.status,
            occurredAt = entity.occurredAt,
        )
    }

    fun toEntity(domain: OutboxEvent): OutboxEventEntity {
        return OutboxEventEntity(
            id = domain.persistenceId,
            eventId = domain.eventId,
            eventType = domain.eventType,
            aggregateId = domain.aggregateId,
            topic = domain.topic,
            partitionKey = domain.partitionKey,
            payload = domain.payload,
            status = domain.status,
            occurredAt = domain.occurredAt,
        )
    }
}
