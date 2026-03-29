package com.loopers.application

import com.loopers.domain.event.EventLog
import com.loopers.domain.metrics.ProductMetricsRepository
import com.loopers.event.AggregateTypes
import com.loopers.event.EventEnvelope
import com.loopers.event.EventTypes
import com.loopers.infrastructure.event.EventHandledJpaRepository
import com.loopers.infrastructure.event.EventLogJpaRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class CatalogEventProcessor(
    private val eventHandledRepository: EventHandledJpaRepository,
    private val eventLogRepository: EventLogJpaRepository,
    private val productMetricsRepository: ProductMetricsRepository,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun process(envelope: EventEnvelope) {
        // 1. 멱등성 체크 — INSERT IGNORE로 원자적 보장
        if (eventHandledRepository.insertIgnore(envelope.eventId) == 0) {
            log.debug("[Catalog] 이미 처리된 이벤트 스킵: eventId={}", envelope.eventId)
            return
        }

        // 2. 최신성 체크
        val productId = envelope.aggregateId.toLong()
        val currentVersion = productMetricsRepository.getVersion(productId)
        if (currentVersion != null && currentVersion >= envelope.version) {
            log.debug(
                "[Catalog] 구버전 이벤트 스킵: eventId={}, current={}, received={}",
                envelope.eventId,
                currentVersion,
                envelope.version,
            )
            return
        }

        // 3. 비즈니스 처리
        when (envelope.eventType) {
            EventTypes.LIKED -> productMetricsRepository.incrementLikeCount(productId, envelope.version)
            EventTypes.UNLIKED -> productMetricsRepository.decrementLikeCount(productId, envelope.version)
            EventTypes.VIEWED -> productMetricsRepository.incrementViewCount(productId, envelope.version)
            else -> log.warn("[Catalog] 알 수 없는 이벤트 타입: {}", envelope.eventType)
        }

        // 4. 처리 완료 기록
        eventLogRepository.save(EventLog.success(envelope, AggregateTypes.CATALOG))
    }
}
