package com.loopers.interfaces.consumer

import com.fasterxml.jackson.databind.ObjectMapper
import com.loopers.application.consumer.DeadLetterPublisher
import com.loopers.application.consumer.EventHandledRecorder
import com.loopers.application.consumer.RawIntegrationEvent
import com.loopers.application.metrics.ProductMetricsUpdater
import com.loopers.application.ranking.RankingUpdater
import com.loopers.config.kafka.KafkaConfig
import com.loopers.kafka.KafkaTopics
import com.loopers.kafka.ProductLikedPayload
import com.loopers.kafka.ProductUnlikedPayload
import com.loopers.kafka.ProductViewedPayload
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Component
class CatalogEventConsumer(
    private val objectMapper: ObjectMapper,
    private val eventHandledRecorder: EventHandledRecorder,
    private val productMetricsUpdater: ProductMetricsUpdater,
    private val rankingUpdater: RankingUpdater,
    private val deadLetterPublisher: DeadLetterPublisher,
) {
    companion object {
        private const val CONSUMER_GROUP = "product-metrics-catalog"
    }

    @KafkaListener(
        topics = [KafkaTopics.CATALOG_EVENTS],
        groupId = CONSUMER_GROUP,
        containerFactory = KafkaConfig.BATCH_LISTENER,
    )
    fun consume(
        messages: List<ConsumerRecord<String, String>>,
        acknowledgment: Acknowledgment,
    ) {
        messages.forEach { record ->
            runCatching {
                val event = objectMapper.readValue(record.value(), RawIntegrationEvent::class.java)
                if (!eventHandledRecorder.markHandled(CONSUMER_GROUP, event.eventId)) {
                    return@runCatching
                }

                when (event.eventType) {
                    "ProductLiked" -> {
                        val payload = objectMapper.treeToValue(event.payload, ProductLikedPayload::class.java)
                        productMetricsUpdater.increaseLikeCount(payload.productId, event.occurredAt)
                        rankingUpdater.applyLikeChanged(payload.productId, payload.delta, event.occurredAt)
                    }

                    "ProductUnliked" -> {
                        val payload = objectMapper.treeToValue(event.payload, ProductUnlikedPayload::class.java)
                        productMetricsUpdater.decreaseLikeCount(payload.productId, event.occurredAt)
                        rankingUpdater.applyLikeChanged(payload.productId, payload.delta, event.occurredAt)
                    }

                    "ProductViewed" -> {
                        val payload = objectMapper.treeToValue(event.payload, ProductViewedPayload::class.java)
                        productMetricsUpdater.increaseViewCount(payload.productId, event.occurredAt)
                        rankingUpdater.applyViewed(payload.productId, event.occurredAt)
                    }
                }
            }.onFailure { ex ->
                deadLetterPublisher.publish(
                    sourceTopic = record.topic(),
                    key = record.key(),
                    payload = record.value(),
                    cause = ex,
                )
            }
        }

        acknowledgment.acknowledge()
    }
}
