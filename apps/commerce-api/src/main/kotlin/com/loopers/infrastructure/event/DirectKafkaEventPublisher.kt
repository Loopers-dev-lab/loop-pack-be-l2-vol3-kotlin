package com.loopers.infrastructure.event

import com.loopers.application.event.DirectEventPublisher
import com.loopers.event.EventEnvelope
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID

@Component
class DirectKafkaEventPublisher(
    private val kafkaTemplate: KafkaTemplate<Any, Any>,
    private val objectMapper: ObjectMapper,
) : DirectEventPublisher {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun publish(topic: String, key: String, eventType: String, payload: Any) {
        try {
            kafkaTemplate.send(
                topic,
                key,
                EventEnvelope(
                    eventId = UUID.randomUUID().toString(),
                    eventType = eventType,
                    aggregateId = key,
                    version = System.currentTimeMillis(),
                    timestamp = Instant.now(),
                    payload = objectMapper.writeValueAsString(payload),
                ),
            )
        } catch (e: Exception) {
            log.warn("[DirectKafkaEventPublisher] 발행 실패: topic={}, key={}, error={}", topic, key, e.message)
        }
    }
}
