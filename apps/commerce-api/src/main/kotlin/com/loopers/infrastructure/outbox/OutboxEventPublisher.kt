package com.loopers.infrastructure.outbox

import com.loopers.domain.outbox.OutboxEventRepository
import com.loopers.event.KafkaEventMessage
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Component
class OutboxEventPublisher(
    private val outboxEventRepository: OutboxEventRepository,
    private val kafkaTemplate: KafkaTemplate<Any, Any>,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val BATCH_SIZE = 100
    }

    @Scheduled(fixedDelay = 1000)
    @Transactional
    fun publishPendingEvents() {
        val pendingEvents = outboxEventRepository.findPendingEvents(BATCH_SIZE)
        if (pendingEvents.isEmpty()) return

        for (event in pendingEvents) {
            try {
                @Suppress("UNCHECKED_CAST")
                val payloadMap = objectMapper.readValue(event.payload, Map::class.java) as Map<String, Any?>
                val message = KafkaEventMessage(
                    eventId = UUID.randomUUID().toString(),
                    eventType = event.eventType,
                    aggregateType = event.aggregateType,
                    aggregateId = event.aggregateId,
                    payload = payloadMap,
                    version = event.id,
                    occurredAt = event.createdAt,
                )

                kafkaTemplate.send(event.topic, event.partitionKey, message)
                    .whenComplete { _, ex ->
                        if (ex != null) {
                            log.error("Kafka 발행 실패: outboxId=${event.id}, topic=${event.topic}", ex)
                        }
                    }

                event.markSent()
            } catch (e: Exception) {
                log.error("Outbox 이벤트 발행 중 오류: outboxId=${event.id}", e)
                event.markFailed()
            }
        }
    }
}
