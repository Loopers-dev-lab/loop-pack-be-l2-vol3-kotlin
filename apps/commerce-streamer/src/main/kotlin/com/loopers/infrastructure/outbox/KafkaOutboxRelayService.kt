package com.loopers.infrastructure.outbox

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class KafkaOutboxRelayService(
    private val kafkaOutboxJpaRepository: KafkaOutboxJpaRepository,
    private val kafkaTemplate: KafkaTemplate<String, KafkaOutboxEnvelope>,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelay = 1_000)
    fun relay() {
        kafkaOutboxJpaRepository.findAllByPublishedAtIsNullOrderByIdAsc().forEach { outbox ->
            val envelope = KafkaOutboxEnvelope(
                eventId = outbox.id!!,
                eventType = outbox.eventType,
                aggregateId = outbox.aggregateId,
                payload = objectMapper.readTree(outbox.payload),
            )

            runCatching {
                kafkaTemplate.send(outbox.topic, outbox.eventKey, envelope).get()
            }.onFailure { exception ->
                log.warn(
                    "Failed to publish outbox event. outboxId={}, topic={}, eventType={}",
                    outbox.id,
                    outbox.topic,
                    outbox.eventType,
                    exception,
                )
                return@forEach
            }

            outbox.markPublished()
            kafkaOutboxJpaRepository.saveAndFlush(outbox)
        }
    }
}
