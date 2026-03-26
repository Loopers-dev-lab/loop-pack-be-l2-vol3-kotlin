package com.loopers.interfaces.consumer

import com.fasterxml.jackson.databind.ObjectMapper
import com.loopers.application.consumer.EventHandledRecorder
import com.loopers.application.consumer.RawIntegrationEvent
import com.loopers.config.kafka.KafkaConfig
import com.loopers.infrastructure.metrics.ProductMetricsEntity
import com.loopers.infrastructure.metrics.ProductMetricsJpaRepository
import com.loopers.kafka.KafkaTopics
import com.loopers.kafka.ProductLikedPayload
import com.loopers.kafka.ProductUnlikedPayload
import com.loopers.kafka.ProductViewedPayload
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class CatalogEventConsumer(
    private val objectMapper: ObjectMapper,
    private val eventHandledRecorder: EventHandledRecorder,
    private val productMetricsJpaRepository: ProductMetricsJpaRepository,
) {
    companion object {
        private const val CONSUMER_GROUP = "product-metrics-catalog"
    }

    @Transactional
    @KafkaListener(
        topics = [KafkaTopics.CATALOG_EVENTS],
        groupId = CONSUMER_GROUP,
        containerFactory = KafkaConfig.MANUAL_LISTENER,
    )
    fun consume(
        message: String,
        acknowledgment: Acknowledgment,
    ) {
        val event = objectMapper.readValue(message, RawIntegrationEvent::class.java)
        if (!eventHandledRecorder.markHandled(CONSUMER_GROUP, event.eventId)) {
            acknowledgment.acknowledge()
            return
        }

        when (event.eventType) {
            "ProductLiked" -> {
                val payload = objectMapper.treeToValue(event.payload, ProductLikedPayload::class.java)
                val metrics = productMetricsJpaRepository.findById(payload.productId)
                    .orElse(ProductMetricsEntity(productId = payload.productId))
                metrics.likeCount += 1
                productMetricsJpaRepository.save(metrics)
            }

            "ProductUnliked" -> {
                val payload = objectMapper.treeToValue(event.payload, ProductUnlikedPayload::class.java)
                val metrics = productMetricsJpaRepository.findById(payload.productId)
                    .orElse(ProductMetricsEntity(productId = payload.productId))
                metrics.likeCount = (metrics.likeCount - 1L).coerceAtLeast(0L)
                productMetricsJpaRepository.save(metrics)
            }

            "ProductViewed" -> {
                val payload = objectMapper.treeToValue(event.payload, ProductViewedPayload::class.java)
                val metrics = productMetricsJpaRepository.findById(payload.productId)
                    .orElse(ProductMetricsEntity(productId = payload.productId))
                metrics.viewCount += 1
                productMetricsJpaRepository.save(metrics)
            }
        }

        acknowledgment.acknowledge()
    }
}
