package com.loopers.infrastructure.outbox

import com.loopers.domain.outbox.OutboxEventModel
import com.loopers.domain.outbox.OutboxEventRepository
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class OutboxRelay(
    private val outboxEventRepository: OutboxEventRepository,
    private val kafkaTemplate: KafkaTemplate<Any, Any>,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val TOPIC = "order.created.v1"
    }

    @Scheduled(fixedDelay = 1000)
    @Transactional
    fun relay() {
        val events = outboxEventRepository.findPendingEvents()
        processEvents(events)
    }

    @Scheduled(fixedDelay = 5000)
    @Transactional
    fun retryFailed() {
        val events = outboxEventRepository.findFailedEvents()
        processEvents(events)
    }

    private fun processEvents(events: List<OutboxEventModel>) {
        for (event in events) {
            try {
                kafkaTemplate.send(TOPIC, event.aggregateId.toString(), event.payload).get()
                event.markSent()
            } catch (e: Exception) {
                log.error("Outbox relay 실패 - eventId: {}, type: {}", event.id, event.eventType, e)
                event.markFailed()
            }
        }
    }
}
