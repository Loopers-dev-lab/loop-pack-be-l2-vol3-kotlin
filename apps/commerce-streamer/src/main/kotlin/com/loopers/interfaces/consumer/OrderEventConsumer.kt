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
            ?: throw CoreException(ErrorType.BAD_REQUEST, "페이로드 파싱 실패: offset=${message.offset()}")
        val eventId = payload["eventId"] as? String
            ?: throw CoreException(ErrorType.BAD_REQUEST, "eventId 누락: offset=${message.offset()}")
        if (eventId.isBlank()) {
            throw CoreException(ErrorType.BAD_REQUEST, "eventId 공백 오류: offset=${message.offset()}")
        }
        val eventType = payload["eventType"] as? String
            ?: throw CoreException(ErrorType.BAD_REQUEST, "eventType 누락: offset=${message.offset()}")
        if (eventType.isBlank()) throw CoreException(ErrorType.BAD_REQUEST, "eventType 공백 오류: eventId=$eventId")
        val productId = (payload["productId"] as? Number)?.toLong()
            ?: throw CoreException(ErrorType.BAD_REQUEST, "productId 누락: offset=${message.offset()}")
        if (productId <= 0) {
            throw CoreException(
                ErrorType.BAD_REQUEST,
                "productId는 양수여야 합니다: eventId=$eventId, productId=$productId",
            )
        }
        val quantity = if (!payload.containsKey("quantity")) {
            log.warn("quantity 필드 누락, 기본값 1L 사용: eventId={}", eventId)
            1L
        } else {
            val raw = payload["quantity"]
            if (raw !is Number) throw CoreException(ErrorType.BAD_REQUEST, "quantity 타입 오류: eventId=$eventId, value=$raw")
            val q = raw.toLong()
            if (q <= 0) throw CoreException(ErrorType.BAD_REQUEST, "quantity는 양수여야 합니다: eventId=$eventId, quantity=$q")
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
