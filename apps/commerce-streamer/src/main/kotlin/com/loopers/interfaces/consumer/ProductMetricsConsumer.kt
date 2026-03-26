package com.loopers.interfaces.consumer

import com.fasterxml.jackson.databind.ObjectMapper
import com.loopers.application.service.ProductMetricsService
import com.loopers.domain.order.event.OrderCreatedEvent
import com.loopers.domain.product.event.ProductViewedEvent
import com.loopers.domain.productlike.event.LikeCountEvent
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Component
class ProductMetricsConsumer(
    private val productMetricsService: ProductMetricsService,
    private val objectMapper: ObjectMapper,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @KafkaListener(
        topics = ["metrics-events"],
        containerFactory = "kafkaListenerContainerFactory",
    )
    fun handleMetricsEvents(
        messages: List<ConsumerRecord<Any, Any>>,
        acknowledgment: Acknowledgment,
    ) {
        try {
            var hasError = false
            for (message in messages) {
                try {
                    val payload = message.value() as String
                    val eventType = detectEventType(payload)

                    when (eventType) {
                        "ProductViewedEvent" -> {
                            val event = objectMapper.readValue(payload, ProductViewedEvent::class.java)
                            productMetricsService.processMetricsEvent(event)
                        }
                        "OrderCreatedEvent" -> {
                            val event = objectMapper.readValue(payload, OrderCreatedEvent::class.java)
                            productMetricsService.processMetricsEvent(event)
                        }
                        "LikeCountEvent" -> {
                            val event = objectMapper.readValue(payload, LikeCountEvent::class.java)
                            productMetricsService.processMetricsEvent(event)
                        }
                        else -> logger.warn("Unknown event type: $eventType")
                    }
                } catch (e: Exception) {
                    logger.error("Failed to process message: ${message.value()}", e)
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

    private fun detectEventType(payload: String): String {
        val tree = objectMapper.readTree(payload)
        return tree.get("type")?.asText()
            ?: tree.get("@class")?.asText()?.substringAfterLast(".")
            ?: "Unknown"
    }
}
