package com.loopers.interfaces.consumer

import com.fasterxml.jackson.databind.ObjectMapper
import com.loopers.application.metrics.MetricsEventProcessor
import com.loopers.config.KafkaTopicConfig
import com.loopers.config.kafka.KafkaConfig
import com.loopers.infrastructure.kafka.RetryableRecordProcessor
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Component
class CatalogEventConsumer(
    private val metricsEventProcessor: MetricsEventProcessor,
    private val retryableRecordProcessor: RetryableRecordProcessor,
    private val objectMapper: ObjectMapper,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val EVENT_TYPE_LIKE_CREATED = "LIKE_CREATED"
        private const val EVENT_TYPE_LIKE_CANCELLED = "LIKE_CANCELLED"
    }

    @KafkaListener(
        topics = [KafkaTopicConfig.CATALOG_EVENTS],
        groupId = "commerce-streamer-catalog",
        containerFactory = KafkaConfig.BATCH_LISTENER,
    )
    fun consume(
        messages: List<ConsumerRecord<String, String>>,
        acknowledgment: Acknowledgment,
    ) {
        messages.forEach { record ->
            retryableRecordProcessor.processWithRetry(record) { rec ->
                val eventId = rec.headerValue("eventId")
                val eventType = rec.headerValue("eventType")

                if (eventId == null || eventType == null) {
                    log.warn("이벤트 헤더 누락 [topic={}, offset={}]", rec.topic(), rec.offset())
                    return@processWithRetry
                }

                val payload = objectMapper.readTree(rec.value())
                val productId = payload.get("productId").asLong()

                when (eventType) {
                    EVENT_TYPE_LIKE_CREATED -> metricsEventProcessor.processLikeCreated(eventId, eventType, productId)
                    EVENT_TYPE_LIKE_CANCELLED -> metricsEventProcessor.processLikeCancelled(eventId, eventType, productId)
                    else -> log.warn("알 수 없는 이벤트 타입 [eventType={}]", eventType)
                }
            }
        }
        acknowledgment.acknowledge()
    }

    private fun ConsumerRecord<String, String>.headerValue(key: String): String? {
        return headers().lastHeader(key)?.value()?.let { String(it) }
    }
}
