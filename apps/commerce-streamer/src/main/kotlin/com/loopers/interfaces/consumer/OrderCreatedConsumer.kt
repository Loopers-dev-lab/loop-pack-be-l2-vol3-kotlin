package com.loopers.interfaces.consumer

import com.fasterxml.jackson.databind.ObjectMapper
import com.loopers.application.service.DlqHandler
import com.loopers.application.service.ProductMetricsService
import com.loopers.config.kafka.KafkaConfig
import com.loopers.domain.event.OrderCreatedEvent
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Component
class OrderCreatedConsumer(
    private val productMetricsService: ProductMetricsService,
    private val dlqHandler: DlqHandler,
    private val objectMapper: ObjectMapper,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @KafkaListener(
        topics = ["order-events"],
        containerFactory = KafkaConfig.METRICS_LISTENER,
    )
    fun handleOrderCreatedEvents(
        messages: List<ConsumerRecord<Any, Any>>,
        acknowledgment: Acknowledgment,
    ) {
        try {
            var hasError = false
            for (message in messages) {
                try {
                    val payload = message.value() as String
                    val event = objectMapper.readValue(payload, OrderCreatedEvent::class.java)
                    productMetricsService.processMetricsEvent(event)
                } catch (e: Exception) {
                    val msg = "Failed to process message from topic=${message.topic()}, " +
                        "partition=${message.partition()}, offset=${message.offset()}"
                    logger.error(msg, e)
                    // ✅ DLQ로 이동
                    val payload = message.value() as? String ?: ""
                    dlqHandler.saveToDlq(
                        originalTopic = "order-events",
                        messagePayload = payload,
                        consumerGroup = "commerce-streamer-order-created",
                        eventType = "OrderCreatedEvent",
                        exception = e,
                    )
                    hasError = true
                }
            }
            if (hasError) {
                logger.warn("Batch completed with errors - see DLQ for details")
            }
        } catch (e: Exception) {
            logger.error("Batch processing failed", e)
        } finally {
            // ✅ 항상 ACK: hasError 여부와 관계없이 배치 처리 완료
            acknowledgment.acknowledge()
        }
    }
}
