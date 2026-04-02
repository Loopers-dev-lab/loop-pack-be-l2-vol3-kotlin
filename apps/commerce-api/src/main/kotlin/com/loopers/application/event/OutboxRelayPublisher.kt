package com.loopers.application.event

import com.fasterxml.jackson.databind.ObjectMapper
import com.loopers.config.kafka.event.CatalogEventMessage
import com.loopers.config.kafka.event.CouponIssueRequestMessage
import com.loopers.infrastructure.outbox.OutboxEventJpaRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@ConditionalOnProperty(prefix = "step2.outbox-relay", name = ["enabled"], havingValue = "true")
class OutboxRelayPublisher(
    private val outboxEventJpaRepository: OutboxEventJpaRepository,
    private val kafkaTemplate: KafkaTemplate<Any, Any>,
    private val objectMapper: ObjectMapper,
    @Value("\${step2.kafka.catalog-topic}") private val catalogTopic: String,
    @Value("\${step3.kafka.coupon-issue-request-topic}") private val couponIssueRequestTopic: String,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelayString = "\${step2.outbox-relay.fixed-delay-ms:1000}")
    @Transactional
    fun publishPendingMessages() {
        outboxEventJpaRepository.findTop100ByPublishedAtIsNullOrderByIdAsc()
            .forEach { outboxEvent ->
                val payload = deserialize(outboxEvent.topic, outboxEvent.payload)
                kafkaTemplate.send(outboxEvent.topic, outboxEvent.partitionKey, payload).get()
                outboxEvent.markPublished()
                log.debug(
                    "outbox_event_published eventId={} topic={} partitionKey={}",
                    outboxEvent.eventId,
                    outboxEvent.topic,
                    outboxEvent.partitionKey,
                )
            }
    }

    private fun deserialize(topic: String, payload: String): Any {
        return when (topic) {
            catalogTopic -> objectMapper.readValue(payload, CatalogEventMessage::class.java)
            couponIssueRequestTopic -> objectMapper.readValue(payload, CouponIssueRequestMessage::class.java)
            else -> throw IllegalStateException("지원하지 않는 outbox topic 입니다: $topic")
        }
    }
}
