package com.loopers.interfaces.consumer

import com.loopers.application.metrics.UpdateProductMetricsUseCase
import com.loopers.config.kafka.KafkaConfig
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Component
class OrderEventConsumer(
    private val updateProductMetricsUseCase: UpdateProductMetricsUseCase,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @KafkaListener(
        topics = [TOPIC],
        containerFactory = KafkaConfig.BATCH_LISTENER,
    )
    fun consume(
        messages: List<ConsumerRecord<Any, Any>>,
        acknowledgment: Acknowledgment,
    ) {
        for (message in messages) {
            try {
                val payload = message.value() as? Map<*, *> ?: continue
                val eventId = payload["eventId"] as? String ?: continue
                val eventType = payload["eventType"] as? String ?: continue
                val productId = (payload["productId"] as? Number)?.toLong() ?: continue
                val quantityRaw = (payload["quantity"] as? Number)?.toLong()
                if (quantityRaw == null) {
                    log.warn("quantity 필드 누락, 기본값 1L 사용: eventId={}", eventId)
                }
                val quantity = quantityRaw ?: 1L

                updateProductMetricsUseCase.handleOrderEvent(
                    eventId = eventId,
                    eventType = eventType,
                    productId = productId,
                    quantity = quantity,
                )
            } catch (ex: Exception) {
                log.error("order-events 처리 실패: offset={}", message.offset(), ex)
            }
        }
        acknowledgment.acknowledge()
    }

    companion object {
        const val TOPIC = "order-events"
    }
}
