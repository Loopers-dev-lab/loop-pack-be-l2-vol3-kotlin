package com.loopers.interfaces.consumer

import com.loopers.application.metrics.UpdateProductMetricsUseCase
import com.loopers.config.kafka.KafkaConfig
import com.loopers.interfaces.consumer.dto.CatalogEventPayload
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class CatalogEventConsumer(
    private val updateProductMetricsUseCase: UpdateProductMetricsUseCase,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @KafkaListener(
        topics = [TOPIC],
        containerFactory = KafkaConfig.RECORD_LISTENER,
    )
    fun consume(payload: CatalogEventPayload) {
        updateProductMetricsUseCase.handleCatalogEvent(
            eventId = payload.eventId,
            eventType = payload.eventType,
            productId = payload.productId,
        )
    }

    companion object {
        const val TOPIC = "catalog-events"
    }
}
