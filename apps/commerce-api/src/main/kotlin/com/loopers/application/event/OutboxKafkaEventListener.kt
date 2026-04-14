package com.loopers.application.event

import com.fasterxml.jackson.databind.ObjectMapper
import com.loopers.domain.event.OutboxEventRepository
import com.loopers.domain.event.OutboxEventSavedEvent
import com.loopers.infrastructure.event.OutboxEventPublisher
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.header.internals.RecordHeader
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class OutboxKafkaEventListener(
    private val kafkaTemplate: KafkaTemplate<Any, Any>,
    private val outboxEventRepository: OutboxEventRepository,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handleOutboxEventSaved(event: OutboxEventSavedEvent) {
        try {
            val payloadNode = objectMapper.readTree(event.payload)
            val record = ProducerRecord<Any, Any>(event.topic, null, event.aggregateId, payloadNode).apply {
                headers().add(RecordHeader(OutboxEventPublisher.HEADER_EVENT_ID, event.outboxEventId.toString().toByteArray()))
                headers().add(RecordHeader(OutboxEventPublisher.HEADER_EVENT_TYPE, event.eventType.toByteArray()))
            }
            kafkaTemplate.send(record).get()
            outboxEventRepository.markPublished(event.outboxEventId)
            log.info("[Outbox] Immediately published: type={} aggregateId={} topic={}", event.eventType, event.aggregateId, event.topic)
        } catch (e: Exception) {
            log.warn("[Outbox] Immediate publish failed, will retry via scheduler: type={} id={}", event.eventType, event.outboxEventId, e)
        }
    }
}
