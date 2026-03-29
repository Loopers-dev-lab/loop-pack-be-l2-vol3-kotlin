package com.loopers.interfaces.consumer

import com.loopers.application.metrics.UpdateProductMetricsUseCase
import com.loopers.config.kafka.KafkaConfig
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.apache.kafka.clients.consumer.ConsumerRecord
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
    fun consume(message: ConsumerRecord<Any, Any>) {
        val payload = message.value() as? Map<*, *>
            ?: throw CoreException(ErrorType.BAD_REQUEST, "페이로드 파싱 실패: offset=${message.offset()}")
        val eventId = payload["eventId"] as? String
            ?: throw CoreException(ErrorType.BAD_REQUEST, "eventId 누락: offset=${message.offset()}")
        val eventType = payload["eventType"] as? String
            ?: throw CoreException(ErrorType.BAD_REQUEST, "eventType 누락: offset=${message.offset()}")
        val productId = (payload["productId"] as? Number)?.toLong()
            ?: throw CoreException(ErrorType.BAD_REQUEST, "productId 누락: offset=${message.offset()}")
        updateProductMetricsUseCase.handleCatalogEvent(
            eventId = eventId,
            eventType = eventType,
            productId = productId,
        )
    }

    companion object {
        const val TOPIC = "catalog-events"
    }
}
