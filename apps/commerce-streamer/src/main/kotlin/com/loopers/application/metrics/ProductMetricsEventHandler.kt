package com.loopers.application.metrics

import com.loopers.config.kafka.event.CatalogEventMessage
import com.loopers.config.kafka.event.CatalogEventType
import com.loopers.domain.metrics.EventHandledModel
import com.loopers.domain.metrics.ProductMetricsModel
import com.loopers.domain.ranking.RankingKeyGenerator
import com.loopers.domain.ranking.RankingRepository
import com.loopers.domain.ranking.RankingScorePolicy
import com.loopers.infrastructure.metrics.EventHandledJpaRepository
import com.loopers.infrastructure.metrics.ProductMetricsDailyJpaRepository
import com.loopers.infrastructure.metrics.ProductMetricsJpaRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.ZoneId

@Component
class ProductMetricsEventHandler(
    private val eventHandledJpaRepository: EventHandledJpaRepository,
    private val productMetricsJpaRepository: ProductMetricsJpaRepository,
    private val productMetricsDailyJpaRepository: ProductMetricsDailyJpaRepository,
    private val rankingRepository: RankingRepository,
    private val rankingScorePolicy: RankingScorePolicy,
) {
    companion object {
        private val KST: ZoneId = ZoneId.of("Asia/Seoul")
    }

    @Transactional
    fun handle(event: CatalogEventMessage) {
        if (!registerEvent(event.eventId)) {
            return
        }

        val metrics = productMetricsJpaRepository.findByProductId(event.productId)
            ?: ProductMetricsModel(productId = event.productId)

        if (metrics.isStale(event)) {
            return
        }

        metrics.apply(event)
        productMetricsJpaRepository.save(metrics)

        upsertDailyMetrics(event)

        val scoreIncrement = rankingScorePolicy.calculateIncrement(event.eventType, event.delta)
        val key = RankingKeyGenerator.dailyKey(event.occurredAt.toLocalDate())
        rankingRepository.incrementScore(key, event.productId, scoreIncrement)
    }

    private fun upsertDailyMetrics(event: CatalogEventMessage) {
        val (likesDelta, viewsDelta, salesDelta) = when (event.eventType) {
            CatalogEventType.LIKE_CHANGED -> Triple(event.delta, 0L, 0L)
            CatalogEventType.PRODUCT_VIEWED -> Triple(0L, event.delta, 0L)
            CatalogEventType.ORDER_COMPLETED -> Triple(0L, 0L, event.delta)
        }
        val metricDate = event.occurredAt
            .withZoneSameInstant(KST)
            .toLocalDate()
        productMetricsDailyJpaRepository.upsert(
            productId = event.productId,
            metricDate = metricDate,
            likesDelta = likesDelta,
            viewsDelta = viewsDelta,
            salesDelta = salesDelta,
        )
    }

    private fun registerEvent(eventId: String): Boolean {
        return runCatching {
            eventHandledJpaRepository.save(EventHandledModel(eventId))
            true
        }.recoverCatching { throwable ->
            if (throwable is DataIntegrityViolationException) {
                false
            } else {
                throw throwable
            }
        }.getOrThrow()
    }
}
