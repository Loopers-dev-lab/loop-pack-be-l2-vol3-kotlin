package com.loopers.interfaces.consumer

import com.loopers.application.service.ProductMetricsService
import com.loopers.domain.product.event.ProductViewedEvent
import com.loopers.domain.productlike.event.LikeCountEvent
import com.loopers.config.kafka.KafkaConfig
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import org.slf4j.LoggerFactory

@Component
class ProductMetricsConsumer(
    private val productMetricsService: ProductMetricsService,
    private val objectMapper: ObjectMapper,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    @KafkaListener(
        topics = ["product.viewed", "like.count"],
        containerFactory = KafkaConfig.BATCH_LISTENER,
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

                    when (message.topic()) {
                        "product.viewed" -> {
                            val event = objectMapper.readValue(payload, ProductViewedEvent::class.java)
                            productMetricsService.processMetricsEvent(event, event.dedupeKey)
                        }
                        "like.count" -> {
                            val event = objectMapper.readValue(payload, LikeCountEvent::class.java)
                            productMetricsService.processMetricsEvent(event, event.dedupeKey)
                        }
                        else -> logger.warn("Unknown topic: ${message.topic()}")
                    }
                } catch (e: Exception) {
                    logger.error("Failed to process message: ${message.value()}", e)
                    hasError = true
                    // Do not acknowledge - let Kafka retry
                }
            }
            // Only acknowledge if all messages were processed successfully
            if (!hasError) {
                acknowledgment.acknowledge()
            }
        } catch (e: Exception) {
            logger.error("Batch processing failed", e)
            // Do not acknowledge - let Kafka retry
        }
    }
}
