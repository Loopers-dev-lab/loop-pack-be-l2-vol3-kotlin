package com.loopers.application.event

import com.fasterxml.jackson.databind.ObjectMapper
import com.loopers.config.kafka.event.CatalogEventMessage
import com.loopers.infrastructure.outbox.OutboxEventJpaRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@ConditionalOnProperty(prefix = "step2.outbox-relay", name = ["enabled"], havingValue = "true")
class OutboxRelayPublisher(
    private val outboxEventJpaRepository: OutboxEventJpaRepository,
    private val kafkaTemplate: KafkaTemplate<Any, Any>,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelayString = "\${step2.outbox-relay.fixed-delay-ms:1000}")
    @Transactional
    fun publishPendingMessages() {
        outboxEventJpaRepository.findTop100ByPublishedAtIsNullOrderByIdAsc()
            .forEach { outboxEvent ->
                val payload = objectMapper.readValue(outboxEvent.payload, CatalogEventMessage::class.java)
                kafkaTemplate.send(outboxEvent.topic, outboxEvent.partitionKey, payload).get()
                outboxEvent.markPublished()
                log.debug("outbox_event_published eventId={} topic={} partitionKey={}", outboxEvent.eventId, outboxEvent.topic, outboxEvent.partitionKey)
            }
    }
}
