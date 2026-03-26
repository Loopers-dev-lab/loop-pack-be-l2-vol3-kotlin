package com.loopers.domain.outbox

import com.loopers.infrastructure.outbox.OutboxEvent
import com.loopers.infrastructure.outbox.OutboxRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class OutboxPublisher(
    private val outboxRepository: OutboxRepository,
    private val objectMapper: ObjectMapper,
) {
    fun publish(event: Any, aggregateId: Long) {
        val payload = objectMapper.writeValueAsString(event)
        val outboxEvent = OutboxEvent(
            aggregateId = aggregateId,
            eventType = event::class.simpleName!!,
            payload = payload,
        )
        outboxRepository.save(outboxEvent)
    }
}
