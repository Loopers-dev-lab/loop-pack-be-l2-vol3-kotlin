package com.loopers.application.metrics

import com.loopers.config.kafka.event.CatalogEventMessage
import com.loopers.domain.metrics.EventHandledModel
import com.loopers.domain.metrics.ProductMetricsModel
import com.loopers.domain.ranking.RankingKeyGenerator
import com.loopers.domain.ranking.RankingRepository
import com.loopers.domain.ranking.RankingScorePolicy
import com.loopers.infrastructure.metrics.EventHandledJpaRepository
import com.loopers.infrastructure.metrics.ProductMetricsJpaRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class ProductMetricsEventHandler(
    private val eventHandledJpaRepository: EventHandledJpaRepository,
    private val productMetricsJpaRepository: ProductMetricsJpaRepository,
    private val rankingRepository: RankingRepository,
    private val rankingScorePolicy: RankingScorePolicy,
) {
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

        val scoreIncrement = rankingScorePolicy.calculateIncrement(event.eventType, event.delta)
        val key = RankingKeyGenerator.dailyKey(event.occurredAt.toLocalDate())
        rankingRepository.incrementScore(key, event.productId, scoreIncrement)
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
