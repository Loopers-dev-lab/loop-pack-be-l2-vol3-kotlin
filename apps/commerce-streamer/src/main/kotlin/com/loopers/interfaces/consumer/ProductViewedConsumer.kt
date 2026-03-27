package com.loopers.interfaces.consumer

import com.fasterxml.jackson.databind.ObjectMapper
import com.loopers.application.service.DlqHandler
import com.loopers.application.service.ProductMetricsService
import com.loopers.config.kafka.KafkaConfig
import com.loopers.domain.event.ProductViewedEvent
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Component
class ProductViewedConsumer(
    private val productMetricsService: ProductMetricsService,
    private val dlqHandler: DlqHandler,
    private val objectMapper: ObjectMapper,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @KafkaListener(
        topics = ["product-events"],
        containerFactory = KafkaConfig.METRICS_LISTENER,
    )
    fun handleProductViewedEvents(
        messages: List<ConsumerRecord<Any, Any>>,
        acknowledgment: Acknowledgment,
    ) {
        try {
            var hasError = false
            for (message in messages) {
                try {
                    val payload = message.value() as String
                    val event = objectMapper.readValue(payload, ProductViewedEvent::class.java)
                    productMetricsService.processMetricsEvent(event)
                } catch (e: Exception) {
                    logger.error("Failed to process message: ${message.value()}", e)
                    // ✅ DLQ로 이동
                    val payload = message.value() as? String ?: ""
                    dlqHandler.saveToDlq(
                        originalTopic = "product-events",
                        messagePayload = payload,
                        consumerGroup = "commerce-streamer-product-viewed",
                        eventType = "ProductViewedEvent",
                        exception = e,
                    )
                    hasError = true
                }
            }
            if (!hasError) {
                acknowledgment.acknowledge()
            }
        } catch (e: Exception) {
            logger.error("Batch processing failed", e)
        }
    }
}
