package com.loopers.interfaces.consumer

import com.loopers.application.metrics.UpdateProductMetricsUseCase
import com.loopers.config.kafka.KafkaConfig
import com.loopers.interfaces.consumer.dto.OrderEventPayload
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
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
    fun consume(payload: OrderEventPayload) {
        if (payload.eventId.isBlank()) {
            throw CoreException(ErrorType.BAD_REQUEST, "eventId 공백 오류")
        }
        if (payload.eventType.isBlank()) {
            throw CoreException(ErrorType.BAD_REQUEST, "eventType 공백 오류: eventId=${payload.eventId}")
        }
        if (payload.productId <= 0) {
            throw CoreException(
                ErrorType.BAD_REQUEST,
                "productId는 양수여야 합니다: eventId=${payload.eventId}, productId=${payload.productId}",
            )
        }
        val quantity = payload.quantity ?: run {
            log.warn("quantity 필드 누락, 기본값 1L 사용: eventId={}", payload.eventId)
            1L
        }
        if (quantity <= 0) {
            throw CoreException(
                ErrorType.BAD_REQUEST,
                "quantity는 양수여야 합니다: eventId=${payload.eventId}, quantity=$quantity",
            )
        }
        updateProductMetricsUseCase.handleOrderEvent(
            eventId = payload.eventId,
            eventType = payload.eventType,
            productId = payload.productId,
            quantity = quantity,
        )
    }

    companion object {
        const val TOPIC = "order-events"
    }
}
