package com.loopers.domain.event

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class OutboxEventService(
    private val outboxEventRepository: OutboxEventRepository,
    private val objectMapper: ObjectMapper,
    private val eventPublisher: ApplicationEventPublisher,
) {
    @Transactional
    fun saveOutboxEvent(
        aggregateType: String,
        aggregateId: String,
        eventType: String,
        topic: String,
        event: Any,
    ): OutboxEvent {
        val payload = objectMapper.writeValueAsString(event)
        val saved = outboxEventRepository.save(
            OutboxEvent(
                aggregateType = aggregateType,
                aggregateId = aggregateId,
                eventType = eventType,
                payload = payload,
                topic = topic,
            ),
        )

        eventPublisher.publishEvent(
            OutboxEventSavedEvent(
                outboxEventId = saved.id,
                aggregateId = saved.aggregateId,
                eventType = saved.eventType,
                payload = saved.payload,
                topic = saved.topic,
            ),
        )

        return saved
    }

    @Transactional(readOnly = true)
    fun findUnpublishedEvents(limit: Int): List<OutboxEvent> {
        return outboxEventRepository.findUnpublishedEvents(limit)
    }
}
