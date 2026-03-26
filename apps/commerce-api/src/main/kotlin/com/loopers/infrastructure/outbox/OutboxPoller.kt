package com.loopers.infrastructure.outbox

import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class OutboxPoller(
    private val outboxRepository: OutboxRepository,
    private val kafkaTemplate: KafkaTemplate<String, String>,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelay = 10000) // 10 seconds
    fun pollAndPublish() {
        val unpublished = outboxRepository.findUnpublished(limit = 100)

        for (outbox in unpublished) {
            try {
                kafkaTemplate.send(
                    outbox.topic,
                    outbox.aggregateId.toString(),
                    outbox.payload,
                ).get()

                outbox.published = true
                outbox.publishedAt = java.time.LocalDateTime.now()
                outboxRepository.save(outbox)

                logger.debug("Published outbox event: id={}, eventType={}", outbox.id, outbox.eventType)
            } catch (e: Exception) {
                logger.error("Failed to publish outbox event: id=${outbox.id}, eventType=${outbox.eventType}", e)
            }
        }
    }
}
