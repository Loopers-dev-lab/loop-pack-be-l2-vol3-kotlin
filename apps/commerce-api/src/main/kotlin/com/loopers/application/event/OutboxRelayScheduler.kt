package com.loopers.application.event

import com.fasterxml.jackson.databind.ObjectMapper
import com.loopers.domain.outbox.OutboxEventRepository
import com.loopers.domain.outbox.OutboxEventStatus
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class OutboxRelayScheduler(
    private val outboxEventRepository: OutboxEventRepository,
    private val kafkaTemplate: KafkaTemplate<Any, Any>,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelay = 1000)
    @Transactional
    fun relay() {
        val events = outboxEventRepository.findPendingEvents(100)
        if (events.isEmpty()) return

        for (event in events) {
            try {
                val payloadMap = objectMapper.readValue(event.payload, Map::class.java).toMutableMap()
                payloadMap["eventType"] = event.eventType
                kafkaTemplate.send(event.topic, event.partitionKey, payloadMap)
                    .whenComplete { _, ex ->
                        if (ex != null) {
                            log.error("Kafka 발행 실패. eventId={}, topic={}", event.eventId, event.topic, ex)
                        }
                    }
                outboxEventRepository.updateStatus(
                    requireNotNull(event.persistenceId),
                    OutboxEventStatus.PUBLISHED,
                )
            } catch (e: Exception) {
                log.error("Outbox relay 실패. eventId={}", event.eventId, e)
                outboxEventRepository.updateStatus(
                    requireNotNull(event.persistenceId),
                    OutboxEventStatus.FAILED,
                )
            }
        }
        log.debug("Outbox relay 완료. count={}", events.size)
    }
}
