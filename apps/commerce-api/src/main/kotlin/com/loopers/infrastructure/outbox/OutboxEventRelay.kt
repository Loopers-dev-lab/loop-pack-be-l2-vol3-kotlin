package com.loopers.infrastructure.outbox

import com.loopers.domain.outbox.OutboxEventRepository
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate

@Component
class OutboxEventRelay(
    private val outboxEventRepository: OutboxEventRepository,
    private val kafkaTemplate: KafkaTemplate<Any, Any>,
    private val transactionTemplate: TransactionTemplate,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val BATCH_SIZE = 100
    }

    @Scheduled(fixedDelay = 1_000)
    fun relay() {
        val events = outboxEventRepository.findUnpublished(BATCH_SIZE)
        if (events.isEmpty()) return

        var successCount = 0
        var failCount = 0

        events.forEach { event ->
            try {
                kafkaTemplate.send(event.topic, event.partitionKey, event.payload).get()

                transactionTemplate.executeWithoutResult {
                    event.markPublished()
                }

                successCount++
            } catch (e: Exception) {
                failCount++
                log.error(
                    "Outbox 릴레이 실패 [eventId={}, topic={}, type={}]",
                    event.eventId,
                    event.topic,
                    event.eventType,
                    e,
                )
            }
        }

        if (successCount > 0) {
            log.info("Outbox 릴레이 완료 [성공={}건, 실패={}건]", successCount, failCount)
        }
    }
}
