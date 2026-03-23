package com.loopers.interfaces.consumer

import com.loopers.application.metrics.UpdateProductMetricsUseCase
import com.loopers.config.kafka.KafkaConfig
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Component
class CatalogEventConsumer(
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
                val payload = message.value() as Map<*, *>
                updateProductMetricsUseCase.handleCatalogEvent(
                    eventId = payload["eventId"] as String,
                    eventType = payload["eventType"] as String,
                    productId = (payload["productId"] as Number).toLong(),
                )
            } catch (ex: Exception) {
                log.error("catalog-events 처리 실패: offset={}", message.offset(), ex)
            }
        }
        acknowledgment.acknowledge()
    }

    companion object {
        const val TOPIC = "catalog-events"
    }
}
