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
                // Kafka partition key: partitionKey 우선 사용, 없으면 aggregateId
                // 쿠폰의 경우 "userId:templateId" 형식 → 같은 사용자의 같은 템플릿 요청은 같은 파티션으로
                val partitionKey = outbox.partitionKey ?: outbox.aggregateId.toString()

                kafkaTemplate.send(
                    outbox.topic,
                    partitionKey,
                    outbox.payload,
                ).get()

                outbox.published = true
                outbox.publishedAt = java.time.LocalDateTime.now()
                outboxRepository.save(outbox)
            } catch (e: Exception) {
                logger.error("Failed to publish outbox event: id=${outbox.id}, eventType=${outbox.eventType}", e)
            }
        }
    }
}
