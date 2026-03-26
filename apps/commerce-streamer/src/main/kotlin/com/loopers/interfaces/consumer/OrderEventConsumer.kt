package com.loopers.interfaces.consumer

import com.fasterxml.jackson.databind.ObjectMapper
import com.loopers.application.consumer.EventHandledRecorder
import com.loopers.application.consumer.RawIntegrationEvent
import com.loopers.config.kafka.KafkaConfig
import com.loopers.infrastructure.metrics.ProductMetricsEntity
import com.loopers.infrastructure.metrics.ProductMetricsJpaRepository
import com.loopers.kafka.KafkaTopics
import com.loopers.kafka.OrderPaidPayload
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class OrderEventConsumer(
    private val objectMapper: ObjectMapper,
    private val eventHandledRecorder: EventHandledRecorder,
    private val productMetricsJpaRepository: ProductMetricsJpaRepository,
) {
    companion object {
        private const val CONSUMER_GROUP = "product-metrics-order"
    }

    @Transactional
    @KafkaListener(
        topics = [KafkaTopics.ORDER_EVENTS],
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

        if (event.eventType == "OrderPaid") {
            val payload = objectMapper.treeToValue(event.payload, OrderPaidPayload::class.java)
            payload.items.forEach { item ->
                val metrics = productMetricsJpaRepository.findById(item.productId)
                    .orElse(ProductMetricsEntity(productId = item.productId))
                metrics.salesCount += item.quantity.toLong()
                productMetricsJpaRepository.save(metrics)
            }
        }

        acknowledgment.acknowledge()
    }
}
