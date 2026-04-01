package com.loopers.interfaces.consumer

import com.fasterxml.jackson.databind.ObjectMapper
import com.loopers.application.metrics.MetricsService
import com.loopers.application.metrics.OrderItemMetrics
import com.loopers.config.kafka.KafkaConfig
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Component
class OrderEventConsumer(
    private val metricsService: MetricsService,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @KafkaListener(
        topics = ["order-events"],
        groupId = "commerce-streamer-order",
        containerFactory = KafkaConfig.BATCH_LISTENER,
    )
    fun consume(
        messages: List<ConsumerRecord<String, ByteArray>>,
        acknowledgment: Acknowledgment,
    ) {
        log.info("[OrderConsumer] ${messages.size}건 수신")

        for (record in messages) {
            try {
                val envelope = objectMapper.readTree(record.value())
                val eventId = envelope.get("eventId").asText()
                val eventType = envelope.get("eventType").asText()
                val payload = objectMapper.readTree(envelope.get("payload").asText())

                when (eventType) {
                    "ORDER_PLACED" -> {
                        val items = payload.get("items").map { item ->
                            OrderItemMetrics(
                                productId = item.get("productId").asLong(),
                                quantity = item.get("quantity").asInt(),
                                price = item.get("price").asInt(),
                            )
                        }
                        metricsService.handleOrderPlaced(eventId, items)
                    }
                    else -> log.warn("[OrderConsumer] 알 수 없는 eventType: $eventType")
                }
            } catch (ex: Exception) {
                log.error("[OrderConsumer] 처리 실패: offset=${record.offset()}, error=${ex.message}", ex)
            }
        }

        acknowledgment.acknowledge()
    }
}
