package com.loopers.application.outbox

import com.fasterxml.jackson.databind.ObjectMapper
import com.loopers.domain.outbox.OutboxEvent
import com.loopers.domain.outbox.OutboxEventRepository
import com.loopers.domain.outbox.OutboxEventType
import org.springframework.stereotype.Component

@Component
class OutboxEventWriter(
    private val outboxEventRepository: OutboxEventRepository,
    private val objectMapper: ObjectMapper,
) {

    fun write(
        eventType: OutboxEventType,
        partitionKey: String,
        payload: Any,
    ): OutboxEvent {
        val outboxEvent = OutboxEvent.create(
            eventType = eventType.name,
            topic = eventType.topic,
            partitionKey = partitionKey,
            payload = objectMapper.writeValueAsString(payload),
        )
        return outboxEventRepository.save(outboxEvent)
    }
}
