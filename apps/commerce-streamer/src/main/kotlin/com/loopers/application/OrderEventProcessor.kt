package com.loopers.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.loopers.domain.event.EventLog
import com.loopers.domain.metrics.ProductMetricsRepository
import com.loopers.event.AggregateTypes
import com.loopers.event.EventEnvelope
import com.loopers.event.EventTypes
import com.loopers.event.payload.OrderCompletedPayload
import com.loopers.infrastructure.coupon.IssuedCouponJpaRepository
import com.loopers.infrastructure.event.EventHandledJpaRepository
import com.loopers.infrastructure.event.EventLogJpaRepository
import com.loopers.infrastructure.product.ProductStockJpaRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class OrderEventProcessor(
    private val eventHandledRepository: EventHandledJpaRepository,
    private val eventLogRepository: EventLogJpaRepository,
    private val productMetricsRepository: ProductMetricsRepository,
    private val productStockRepository: ProductStockJpaRepository,
    private val issuedCouponRepository: IssuedCouponJpaRepository,
    private val objectMapper: ObjectMapper,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun process(envelope: EventEnvelope) {
        // 1. 멱등성 체크 — INSERT IGNORE로 원자적 보장
        if (eventHandledRepository.insertIgnore(envelope.eventId) == 0) {
            log.debug("[Order] 이미 처리된 이벤트 스킵: eventId={}", envelope.eventId)
            return
        }

        // 2. 비즈니스 처리
        when (envelope.eventType) {
            EventTypes.ORDER_COMPLETED -> {
                val payload = objectMapper.readValue(envelope.payload, OrderCompletedPayload::class.java)

                payload.items.forEach { item ->
                    productStockRepository.decrementStock(item.productId, item.quantity)
                    productMetricsRepository.incrementSalesCount(item.productId, item.quantity)
                }

                payload.couponId?.let { couponId ->
                    issuedCouponRepository.markUsed(couponId, payload.userId)
                }
            }
            else -> log.warn("[Order] 알 수 없는 이벤트 타입: {}", envelope.eventType)
        }

        // 3. 처리 완료 기록
        eventLogRepository.save(EventLog.success(envelope, AggregateTypes.ORDER))
    }
}
