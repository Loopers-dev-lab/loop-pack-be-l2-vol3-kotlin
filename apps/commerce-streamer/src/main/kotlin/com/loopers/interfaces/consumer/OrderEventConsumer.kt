package com.loopers.interfaces.consumer

import com.fasterxml.jackson.databind.ObjectMapper
import com.loopers.application.consumer.DeadLetterPublisher
import com.loopers.application.consumer.EventHandledRecorder
import com.loopers.application.consumer.RawIntegrationEvent
import com.loopers.application.metrics.ProductMetricsUpdater
import com.loopers.application.ranking.RankingUpdater
import com.loopers.config.kafka.KafkaConfig
import com.loopers.kafka.KafkaTopics
import com.loopers.kafka.OrderPaidPayload
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Component
class OrderEventConsumer(
    private val objectMapper: ObjectMapper,
    private val eventHandledRecorder: EventHandledRecorder,
    private val productMetricsUpdater: ProductMetricsUpdater,
    private val rankingUpdater: RankingUpdater,
    private val deadLetterPublisher: DeadLetterPublisher,
) {
    companion object {
        private const val CONSUMER_GROUP = "product-metrics-order"
    }

    @KafkaListener(
        topics = [KafkaTopics.ORDER_EVENTS],
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

                if (event.eventType == "OrderPaid") {
                    val payload = objectMapper.treeToValue(event.payload, OrderPaidPayload::class.java)
                    payload.items.forEach { item ->
                        productMetricsUpdater.increaseSalesCount(
                            productId = item.productId,
                            quantity = item.quantity.toLong(),
                            occurredAt = event.occurredAt,
                        )
                        rankingUpdater.applyOrdered(
                            productId = item.productId,
                            quantity = item.quantity.toLong(),
                            occurredAt = event.occurredAt,
                        )
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
