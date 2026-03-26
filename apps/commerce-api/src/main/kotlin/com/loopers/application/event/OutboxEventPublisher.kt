package com.loopers.application.event

import com.loopers.infrastructure.outbox.OutboxEventJpaRepository
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.ZonedDateTime

@Component
class OutboxEventPublisher(
    private val outboxEventJpaRepository: OutboxEventJpaRepository,
    private val kafkaTemplate: KafkaTemplate<String, String>,
) {
    private val log = LoggerFactory.getLogger(OutboxEventPublisher::class.java)

    @Scheduled(fixedDelayString = "\${outbox.publish.fixed-delay-ms:1000}")
    fun publishPending() {
        outboxEventJpaRepository.findTop100ByPublishedAtIsNullOrderByIdAsc()
            .forEach { entity ->
                try {
                    kafkaTemplate.send(entity.topic, entity.messageKey, entity.payload).get()
                    entity.publishedAt = ZonedDateTime.now()
                    outboxEventJpaRepository.save(entity)
                } catch (ex: Exception) {
                    entity.attemptCount += 1
                    outboxEventJpaRepository.save(entity)
                    log.warn("outbox publish failed eventId={} topic={}", entity.eventId, entity.topic, ex)
                }
            }
    }
}
