package com.loopers.application.service

import com.loopers.domain.eventhandled.EventHandledDto
import com.loopers.domain.eventhandled.EventHandledRepository
import com.loopers.domain.event.OrderCreatedEvent
import com.loopers.domain.event.ProductViewedEvent
import com.loopers.infrastructure.productmetrics.ProductMetricsRepository
import com.loopers.interfaces.consumer.EventHandler
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class ProductMetricsService(
    private val productMetricsRepository: ProductMetricsRepository,
    private val eventHandledRepository: EventHandledRepository,
    private val handlers: Map<String, EventHandler>,
) {
    fun processMetricsEvent(event: Any) {
        val dedupeKey = extractDedupeKey(event)

        // 1. 멱등성 검증 - 이미 처리된 이벤트면 return
        if (eventHandledRepository.existsByDedupeKey(dedupeKey)) {
            return
        }

        // 2. 이벤트 타입별 핸들러 호출
        val eventClassName = event::class.simpleName ?: return
        val handler = handlers[eventClassName]
        handler?.handle(event)

        // 3. event_handled 기록 (멱등성 완료)
        eventHandledRepository.save(EventHandledDto(dedupeKey = dedupeKey))
    }

    private fun extractDedupeKey(event: Any): String = when (event) {
        is ProductViewedEvent -> event.dedupeKey
        is OrderCreatedEvent -> event.dedupeKey
        else -> throw IllegalArgumentException("Unknown event type: ${event::class.simpleName}")
    }
}
