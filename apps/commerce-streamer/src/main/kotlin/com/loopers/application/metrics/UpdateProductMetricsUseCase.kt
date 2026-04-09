package com.loopers.application.metrics

import com.loopers.domain.event.model.EventHandled
import com.loopers.domain.event.repository.EventHandledRepository
import com.loopers.domain.metrics.model.ProductMetrics
import com.loopers.domain.metrics.repository.ProductMetricsRepository
import com.loopers.domain.ranking.RankingWeight
import com.loopers.domain.ranking.model.FailedScoreUpdate
import com.loopers.domain.ranking.repository.FailedScoreUpdateRepository
import com.loopers.domain.ranking.repository.RankingScoreRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.time.Clock
import java.time.LocalDate

@Component
class UpdateProductMetricsUseCase(
    private val productMetricsRepository: ProductMetricsRepository,
    private val eventHandledRepository: EventHandledRepository,
    private val rankingScoreRepository: RankingScoreRepository,
    private val failedScoreUpdateRepository: FailedScoreUpdateRepository,
    private val clock: Clock,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun handleCatalogEvent(eventId: String, eventType: String, productId: Long) {
        if (eventHandledRepository.existsByEventId(eventId)) {
            log.debug("이미 처리된 이벤트: eventId={}", eventId)
            return
        }

        val metrics = findOrCreate(productId)

        val rankingScore = when (eventType) {
            PRODUCT_VIEWED -> {
                metrics.incrementViewCount()
                RankingWeight.VIEW
            }
            LIKE_ADDED -> {
                metrics.incrementLikeCount()
                RankingWeight.LIKE
            }
            LIKE_REMOVED -> {
                metrics.decrementLikeCount()
                RankingWeight.LIKE * -1
            }
            else -> {
                log.warn("알 수 없는 catalog 이벤트 타입: eventType={}", eventType)
                eventHandledRepository.save(EventHandled(eventId = eventId))
                return
            }
        }

        productMetricsRepository.save(metrics)
        eventHandledRepository.save(EventHandled(eventId = eventId))

        if (rankingScore != 0.0) {
            val rankingDate = LocalDate.now(clock)
            val failedRecord = failedScoreUpdateRepository.save(
                FailedScoreUpdate(eventId = eventId, productId = productId, score = rankingScore, rankingDate = rankingDate),
            )
            registerScoreUpdateAfterCommit(eventId, productId, rankingScore, rankingDate, failedRecord.id)
        }
    }

    @Transactional
    fun handleOrderEvent(eventId: String, eventType: String, productId: Long, quantity: Long) {
        if (eventHandledRepository.existsByEventId(eventId)) {
            log.debug("이미 처리된 이벤트: eventId={}", eventId)
            return
        }

        if (eventType != PAYMENT_COMPLETED) {
            log.warn("알 수 없는 order 이벤트 타입: eventType={}", eventType)
            eventHandledRepository.save(EventHandled(eventId = eventId))
            return
        }

        val metrics = findOrCreate(productId)
        metrics.incrementSalesCount(quantity)

        val score = RankingWeight.ORDER * quantity
        productMetricsRepository.save(metrics)
        eventHandledRepository.save(EventHandled(eventId = eventId))

        val rankingDate = LocalDate.now(clock)
        val failedRecord = failedScoreUpdateRepository.save(
            FailedScoreUpdate(eventId = eventId, productId = productId, score = score, rankingDate = rankingDate),
        )
        registerScoreUpdateAfterCommit(eventId, productId, score, rankingDate, failedRecord.id)
    }

    private fun registerScoreUpdateAfterCommit(
        eventId: String,
        productId: Long,
        score: Double,
        rankingDate: LocalDate,
        failedRecordId: Long,
    ) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                object : TransactionSynchronization {
                    override fun afterCommit() {
                        tryRedisAndCleanup(eventId, productId, score, rankingDate, failedRecordId)
                    }
                },
            )
        } else {
            tryRedisAndCleanup(eventId, productId, score, rankingDate, failedRecordId)
        }
    }

    private fun tryRedisAndCleanup(
        eventId: String,
        productId: Long,
        score: Double,
        rankingDate: LocalDate,
        failedRecordId: Long,
    ) {
        var lastException: Exception? = null
        repeat(MAX_RETRY_COUNT) { attempt ->
            try {
                rankingScoreRepository.incrementScore(productId, score, eventId, rankingDate)
                deleteFailedRecord(failedRecordId)
                return
            } catch (e: Exception) {
                lastException = e
                log.warn(
                    "Redis 랭킹 점수 갱신 재시도 {}/{}. productId={}, score={}: {}",
                    attempt + 1,
                    MAX_RETRY_COUNT,
                    productId,
                    score,
                    e.message,
                )
                Thread.sleep(RETRY_DELAY_MS)
            }
        }
        log.error(
            "Redis 랭킹 점수 갱신 최종 실패, 스케줄러에서 재처리 예정. eventId={}, productId={}: {}",
            eventId,
            productId,
            lastException?.message,
            lastException,
        )
    }

    private fun deleteFailedRecord(failedRecordId: Long) {
        try {
            failedScoreUpdateRepository.deleteById(failedRecordId)
        } catch (e: Exception) {
            log.warn("FailedScoreUpdate 삭제 실패 (스케줄러에서 멱등 재처리 예정). id={}: {}", failedRecordId, e.message)
        }
    }

    private fun findOrCreate(productId: Long): ProductMetrics {
        return productMetricsRepository.findByProductId(productId)
            ?: ProductMetrics(productId = productId)
    }

    companion object {
        const val PRODUCT_VIEWED = "PRODUCT_VIEWED"
        const val LIKE_ADDED = "LIKE_ADDED"
        const val LIKE_REMOVED = "LIKE_REMOVED"
        const val PAYMENT_COMPLETED = "PAYMENT_COMPLETED"
        private const val MAX_RETRY_COUNT = 3
        private const val RETRY_DELAY_MS = 100L
    }
}
