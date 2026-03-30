package com.loopers.infrastructure.outbox

import com.loopers.event.AggregateTypes
import com.loopers.event.EventEnvelope
import com.loopers.event.Topics
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.concurrent.TimeUnit

/**
 * Outbox Relay.
 *
 * 미발행 outbox_events를 폴링하여 Kafka에 발행한다.
 * 발행 성공 시 publishedAt을 기록한다.
 */
@Component
@Profile("!test")
class OutboxRelayScheduler(
    private val outboxEventRepository: OutboxEventRepository,
    private val kafkaTemplate: KafkaTemplate<Any, Any>,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedRate = 1000)
    fun publishPendingEvents() {
        val events = outboxEventRepository.findByPublishedAtIsNull(limit = 100)
        val publishedEvents = mutableListOf<OutboxEvent>()
        events.forEach { event ->
            try {
                kafkaTemplate.send(
                    topicFor(event.aggregateType),
                    event.aggregateId,
                    toEventEnvelope(event),
                ).get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                event.publishedAt = Instant.now()
                publishedEvents.add(event)
            } catch (e: Exception) {
                log.error("[Outbox Relay] 발행 실패: eventId={}, error={}", event.id, e.message)
            }
        }
        if (publishedEvents.isNotEmpty()) {
            outboxEventRepository.saveAll(publishedEvents)
        }
    }

    companion object {
        private const val SEND_TIMEOUT_SECONDS = 5L

        private val TOPIC_MAP = mapOf(
            AggregateTypes.CATALOG to Topics.CATALOG,
            AggregateTypes.ORDER to Topics.ORDER,
            AggregateTypes.COUPON to Topics.COUPON_ISSUE,
        )

        fun topicFor(aggregateType: String): String {
            return TOPIC_MAP[aggregateType]
                ?: throw IllegalArgumentException("알 수 없는 aggregateType: $aggregateType")
        }

        fun toEventEnvelope(event: OutboxEvent): EventEnvelope {
            return EventEnvelope(
                eventId = event.id.toString(),
                eventType = event.eventType,
                aggregateId = event.aggregateId,
                version = event.version,
                timestamp = event.createdAt,
                payload = event.payload,
            )
        }
    }
}
