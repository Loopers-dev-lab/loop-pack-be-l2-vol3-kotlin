package com.loopers.infrastructure.outbox

import com.fasterxml.jackson.databind.ObjectMapper
import com.loopers.application.outbox.OutboxEventPublisher
import com.loopers.domain.common.outbox.OutboxEvent
import com.loopers.domain.common.outbox.OutboxEventRepository
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class OutboxEventPublisherImpl(
    private val outboxEventRepository: OutboxEventRepository,
    private val objectMapper: ObjectMapper,
) : OutboxEventPublisher {

    override fun publish(
        aggregateType: String,
        aggregateId: String,
        eventType: String,
        payload: Map<String, Any?>,
        partitionKey: String,
        topic: String,
    ) {
        outboxEventRepository.save(
            OutboxEvent(
                id = UUID.randomUUID().toString(),
                aggregateType = aggregateType,
                aggregateId = aggregateId,
                eventType = eventType,
                payload = objectMapper.writeValueAsString(payload),
                partitionKey = partitionKey,
                topic = topic,
            ),
        )
    }
}
