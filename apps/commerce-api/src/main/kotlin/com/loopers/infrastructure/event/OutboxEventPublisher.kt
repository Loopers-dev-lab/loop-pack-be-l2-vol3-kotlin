package com.loopers.infrastructure.event

import com.fasterxml.jackson.databind.ObjectMapper
import com.loopers.domain.event.OutboxEventService
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.header.internals.RecordHeader
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class OutboxEventPublisher(
    private val outboxEventService: OutboxEventService,
    private val kafkaTemplate: KafkaTemplate<Any, Any>,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        const val HEADER_EVENT_ID = "outbox-event-id"
        const val HEADER_EVENT_TYPE = "outbox-event-type"
    }

    @Scheduled(fixedDelay = 1000)
    @Transactional
    fun publishPendingEvents() {
        val events = outboxEventService.findUnpublishedEvents(100)
        events.forEach { event ->
            try {
                val payloadNode = objectMapper.readTree(event.payload)
                val record = ProducerRecord<Any, Any>(event.topic, null, event.aggregateId, payloadNode).apply {
                    headers().add(RecordHeader(HEADER_EVENT_ID, event.id.toString().toByteArray()))
                    headers().add(RecordHeader(HEADER_EVENT_TYPE, event.eventType.toByteArray()))
                }
                kafkaTemplate.send(record)
                event.markPublished()
                log.info(
                    "[Outbox] Published event: type={} aggregateId={} topic={}",
                    event.eventType,
                    event.aggregateId,
                    event.topic,
                )
            } catch (e: Exception) {
                log.error("[Outbox] Failed to publish event: id={} type={}", event.id, event.eventType, e)
            }
        }
    }
}
