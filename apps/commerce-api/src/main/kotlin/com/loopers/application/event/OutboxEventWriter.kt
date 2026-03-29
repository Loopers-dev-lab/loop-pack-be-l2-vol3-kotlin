package com.loopers.application.event

import com.fasterxml.jackson.databind.ObjectMapper
import com.loopers.infrastructure.outbox.OutboxEventEntity
import com.loopers.infrastructure.outbox.OutboxEventJpaRepository
import com.loopers.kafka.IntegrationEvent
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Component

@Component
class OutboxEventWriter(
    private val outboxEventJpaRepository: OutboxEventJpaRepository,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(OutboxEventWriter::class.java)

    fun append(topic: String, event: IntegrationEvent<*>) {
        val payload = objectMapper.writeValueAsString(event)
        try {
            outboxEventJpaRepository.save(
                OutboxEventEntity(
                    eventId = event.eventId,
                    topic = topic,
                    messageKey = event.key,
                    eventType = event.eventType,
                    payload = payload,
                ),
            )
        } catch (_: DataIntegrityViolationException) {
            log.debug("duplicate outbox event skipped eventId={}", event.eventId)
        }
    }
}
