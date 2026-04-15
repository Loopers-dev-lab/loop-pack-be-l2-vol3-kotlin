package com.loopers.application.metrics

import com.loopers.domain.event.model.EventHandled
import com.loopers.domain.event.repository.EventHandledRepository
import com.loopers.domain.lock.ProductLockRepository
import com.loopers.domain.metrics.model.ProductMetrics
import com.loopers.domain.metrics.model.ProductMetricsDaily
import com.loopers.domain.metrics.repository.ProductMetricsDailyRepository
import com.loopers.domain.metrics.repository.ProductMetricsRepository
import com.loopers.domain.ranking.RankingWeight
import com.loopers.domain.ranking.model.FailedScoreUpdate
import com.loopers.domain.ranking.repository.FailedScoreUpdateRepository
import com.loopers.domain.ranking.repository.RankingScoreRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.time.Clock
import java.time.LocalDate

@Component
class UpdateProductMetricsUseCase(
    private val productMetricsRepository: ProductMetricsRepository,
    private val productMetricsDailyRepository: ProductMetricsDailyRepository,
    private val productLockRepository: ProductLockRepository,
    private val eventHandledRepository: EventHandledRepository,
    private val rankingScoreRepository: RankingScoreRepository,
    private val failedScoreUpdateRepository: FailedScoreUpdateRepository,
    private val clock: Clock,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional(isolation = Isolation.READ_COMMITTED)
    fun handleCatalogEvent(eventId: String, eventType: String, productId: Long) {
        if (eventHandledRepository.existsByEventId(eventId)) {
            log.debug("이미 처리된 이벤트: eventId={}", eventId)
            return
        }

        if (productLockRepository.findByIdForUpdate(productId) == null) {
            log.warn("상품을 찾을 수 없음 (lock 실패): productId={}", productId)
            eventHandledRepository.save(EventHandled(eventId = eventId))
            return
        }

        // post-check (double-checked locking): lock 획득 후 event_handled를 재확인한다.
        // Kafka 동시 재전달로 같은 eventId가 병렬 도달한 경우 T2가 여기서 조기 종료되어
        // 카운터 중복 증가를 차단한다. Spring Data JPA의 save()는 @Id non-null 엔티티에 대해
        // merge를 호출하므로 `event_handled` PK 충돌로 자연 차단되지 않음 — post-check 필수.
        if (eventHandledRepository.existsByEventId(eventId)) {
            log.debug("락 획득 후 중복 이벤트 감지, skip: eventId={}", eventId)
            return
        }

        if (eventType !in CATALOG_EVENT_TYPES) {
            log.warn("알 수 없는 catalog 이벤트 타입: eventType={}", eventType)
            eventHandledRepository.save(EventHandled(eventId = eventId))
            return
        }

        val metrics = findOrCreate(productId)
        val today = LocalDate.now(clock)
        val daily = findOrCreateDaily(today, productId)

        val rankingScore = when (eventType) {
            PRODUCT_VIEWED -> {
                metrics.incrementViewCount()
                daily.incrementViewCount()
                RankingWeight.VIEW
            }
            LIKE_ADDED -> {
                metrics.incrementLikeCount()
                daily.incrementLikeCount()
                RankingWeight.LIKE
            }
            LIKE_REMOVED -> {
                metrics.decrementLikeCount()
                daily.decrementLikeCount()
                RankingWeight.LIKE * -1
            }
            else -> error("unreachable: filtered by CATALOG_EVENT_TYPES guard")
        }

        productMetricsRepository.save(metrics)
        productMetricsDailyRepository.save(daily)
        eventHandledRepository.save(EventHandled(eventId = eventId))

        if (rankingScore != 0.0) {
            val rankingDate = today
            val failedRecord = failedScoreUpdateRepository.save(
                FailedScoreUpdate(eventId = eventId, productId = productId, score = rankingScore, rankingDate = rankingDate),
            )
            registerScoreUpdateAfterCommit(eventId, productId, rankingScore, rankingDate, failedRecord.id)
        }
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    fun handleOrderEvent(eventId: String, eventType: String, productId: Long, quantity: Long) {
        if (eventHandledRepository.existsByEventId(eventId)) {
            log.debug("이미 처리된 이벤트: eventId={}", eventId)
            return
        }

        if (productLockRepository.findByIdForUpdate(productId) == null) {
            log.warn("상품을 찾을 수 없음 (lock 실패): productId={}", productId)
            eventHandledRepository.save(EventHandled(eventId = eventId))
            return
        }

        // post-check (double-checked locking) — handleCatalogEvent 주석 참조.
        if (eventHandledRepository.existsByEventId(eventId)) {
            log.debug("락 획득 후 중복 이벤트 감지, skip: eventId={}", eventId)
            return
        }

        if (eventType != PAYMENT_COMPLETED) {
            log.warn("알 수 없는 order 이벤트 타입: eventType={}", eventType)
            eventHandledRepository.save(EventHandled(eventId = eventId))
            return
        }

        val metrics = findOrCreate(productId)
        val today = LocalDate.now(clock)
        val daily = findOrCreateDaily(today, productId)
        metrics.incrementSalesCount(quantity)
        daily.incrementSalesCount(quantity)

        val score = RankingWeight.ORDER * quantity
        productMetricsRepository.save(metrics)
        productMetricsDailyRepository.save(daily)
        eventHandledRepository.save(EventHandled(eventId = eventId))

        val rankingDate = today
        val failedRecord = failedScoreUpdateRepository.save(
            FailedScoreUpdate(eventId = eventId, productId = productId, score = score, rankingDate = rankingDate),
        )
        registerScoreUpdateAfterCommit(eventId, productId, score, rankingDate, failedRecord.id)
    }

    private fun findOrCreate(productId: Long): ProductMetrics {
        return productMetricsRepository.findByProductId(productId)
            ?: productMetricsRepository.save(ProductMetrics(productId = productId))
    }

    private fun findOrCreateDaily(date: LocalDate, productId: Long): ProductMetricsDaily {
        return productMetricsDailyRepository.findByDateAndProductId(date, productId)
            ?: productMetricsDailyRepository.save(ProductMetricsDaily(productId = productId, metricDate = date))
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
        try {
            rankingScoreRepository.incrementScore(productId, score, eventId, rankingDate)
            deleteFailedRecord(failedRecordId)
        } catch (e: Exception) {
            log.error(
                "Redis 랭킹 점수 갱신 실패, 스케줄러에서 재처리 예정. eventId={}, productId={}: {}",
                eventId,
                productId,
                e.message,
                e,
            )
        }
    }

    private fun deleteFailedRecord(failedRecordId: Long) {
        try {
            failedScoreUpdateRepository.deleteById(failedRecordId)
        } catch (e: Exception) {
            log.warn("FailedScoreUpdate 삭제 실패 (스케줄러에서 멱등 재처리 예정). id={}: {}", failedRecordId, e.message)
        }
    }

    companion object {
        const val PRODUCT_VIEWED = "PRODUCT_VIEWED"
        const val LIKE_ADDED = "LIKE_ADDED"
        const val LIKE_REMOVED = "LIKE_REMOVED"
        const val PAYMENT_COMPLETED = "PAYMENT_COMPLETED"

        private val CATALOG_EVENT_TYPES = setOf(PRODUCT_VIEWED, LIKE_ADDED, LIKE_REMOVED)
    }
}
