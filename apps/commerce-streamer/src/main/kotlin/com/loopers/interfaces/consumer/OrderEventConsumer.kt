package com.loopers.interfaces.consumer

import com.loopers.application.metrics.UpdateProductMetricsUseCase
import com.loopers.config.kafka.KafkaConfig
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class OrderEventConsumer(
    private val updateProductMetricsUseCase: UpdateProductMetricsUseCase,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @KafkaListener(
        topics = [TOPIC],
        containerFactory = KafkaConfig.RECORD_LISTENER,
    )
    fun consume(message: ConsumerRecord<Any, Any>) {
        val payload = message.value() as? Map<*, *>
            ?: throw IllegalArgumentException("페이로드 파싱 실패: offset=${message.offset()}")
        val eventId = payload["eventId"] as? String
            ?: throw IllegalArgumentException("eventId 누락: offset=${message.offset()}")
        val eventType = payload["eventType"] as? String
            ?: throw IllegalArgumentException("eventType 누락: offset=${message.offset()}")
        val productId = (payload["productId"] as? Number)?.toLong()
            ?: throw IllegalArgumentException("productId 누락: offset=${message.offset()}")
        val quantity = if (!payload.containsKey("quantity")) {
            log.warn("quantity 필드 누락, 기본값 1L 사용: eventId={}", eventId)
            1L
        } else {
            val raw = payload["quantity"]
            require(raw is Number) { "quantity 타입 오류: eventId=$eventId, value=$raw" }
            val q = raw.toLong()
            require(q > 0) { "quantity는 양수여야 합니다: eventId=$eventId, quantity=$q" }
            q
        }
        updateProductMetricsUseCase.handleOrderEvent(
            eventId = eventId,
            eventType = eventType,
            productId = productId,
            quantity = quantity,
        )
    }

    companion object {
        const val TOPIC = "order-events"
    }
}
