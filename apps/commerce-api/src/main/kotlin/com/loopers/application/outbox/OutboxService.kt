package com.loopers.application.outbox

import com.loopers.domain.outbox.OutboxEvent
import com.loopers.domain.outbox.OutboxEventRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component

@Component
class OutboxService(
    private val outboxEventRepository: OutboxEventRepository,
    private val objectMapper: ObjectMapper,
) {

    fun save(
        aggregateType: String,
        aggregateId: String,
        eventType: String,
        topic: String,
        partitionKey: String,
        payload: Any,
    ) {
        outboxEventRepository.save(
            OutboxEvent(
                aggregateType = aggregateType,
                aggregateId = aggregateId,
                eventType = eventType,
                topic = topic,
                partitionKey = partitionKey,
                payload = objectMapper.writeValueAsString(payload),
            ),
        )
    }
}
