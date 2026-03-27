package com.loopers.interfaces.consumer

import com.loopers.application.event.IdempotencyService
import com.loopers.application.metrics.MetricsAggregationService
import com.loopers.config.kafka.KafkaConfig
import com.loopers.event.KafkaEventMessage
import com.loopers.event.KafkaTopics
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Component
class CatalogEventConsumer(
    private val idempotencyService: IdempotencyService,
    private val metricsAggregationService: MetricsAggregationService,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @KafkaListener(
        topics = [KafkaTopics.CATALOG_EVENTS],
        groupId = "metrics-consumer",
        containerFactory = KafkaConfig.BATCH_LISTENER,
    )
    fun consume(messages: List<ConsumerRecord<Any, Any>>, acknowledgment: Acknowledgment) {
        for (record in messages) {
            try {
                val message = objectMapper.readValue(record.value() as ByteArray, KafkaEventMessage::class.java)

                if (idempotencyService.isAlreadyHandled(message.eventId)) {
                    continue
                }

                val productId = message.aggregateId.toLong()

                when (message.eventType) {
                    "PRODUCT_LIKED" -> metricsAggregationService.incrementLikeCount(productId, message.version)
                    "PRODUCT_UNLIKED" -> metricsAggregationService.decrementLikeCount(productId, message.version)
                    "PRODUCT_VIEWED" -> metricsAggregationService.incrementViewCount(productId, message.version)
                    else -> log.warn("알 수 없는 catalog 이벤트 타입: ${message.eventType}")
                }

                idempotencyService.markHandled(
                    eventId = message.eventId,
                    aggregateType = message.aggregateType,
                    aggregateId = message.aggregateId,
                    eventType = message.eventType,
                )
            } catch (e: Exception) {
                log.error("catalog 이벤트 처리 실패: record offset=${record.offset()}", e)
            }
        }
        acknowledgment.acknowledge()
    }
}
