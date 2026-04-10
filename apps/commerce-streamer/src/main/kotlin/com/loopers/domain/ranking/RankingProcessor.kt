package com.loopers.domain.ranking

import com.loopers.domain.metrics.ProductMetricsRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 랭킹 점수 갱신 서비스 (Repository 주입 있음).
 *
 * MetricsConsumer에서 batch 처리 후 호출된다.
 * product_metrics(SSOT)를 조회하여 score를 재계산하고, Redis ZSET에 ZADD한다.
 *
 * 가중치는 Redis의 RankingWeightsRepository에서 조회한다.
 * - 조회 성공: 저장된 가중치 적용
 * - 조회 실패 or 값 없음: RankingWeights.DEFAULT fallback
 *
 * Redis 실패 시 예외를 삼켜서 DB 집계에 영향을 주지 않는다.
 * ZSET 데이터는 파생 데이터이므로, 다음 배치에서 자연 보정된다.
 */
@Component
class RankingProcessor(
    private val productMetricsRepository: ProductMetricsRepository,
    private val rankingRepository: RankingRepository,
    private val rankingWeightsRepository: RankingWeightsRepository,
) {

    companion object {
        private val log = LoggerFactory.getLogger(RankingProcessor::class.java)
        private val DATE_KEY_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd")
        private val HOUR_KEY_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHH")
    }

    fun updateRankings(productIds: Set<Long>) {
        if (productIds.isEmpty()) return

        val now = LocalDateTime.now()
        val dateKey = now.format(DATE_KEY_FORMAT)
        val hourKey = now.format(HOUR_KEY_FORMAT)
        val weights = resolveWeights()
        val metricsList = productMetricsRepository.findByProductIds(productIds)

        if (metricsList.isEmpty()) {
            log.debug("[랭킹] metrics 데이터 없음 [productIds={}]", productIds)
            return
        }

        val scores = metricsList.associate { metrics ->
            metrics.productId to RankingScoreCalculator.calculate(
                viewCount = metrics.viewCount,
                likeCount = metrics.likeCount,
                orderCount = metrics.orderCount,
                weights = weights,
            )
        }

        try {
            rankingRepository.updateScores(dateKey, hourKey, scores)
            log.info(
                "[랭킹] ZSET 갱신 완료 [dateKey={}, hourKey={}, count={}]",
                dateKey,
                hourKey,
                scores.size,
            )
        } catch (e: Exception) {
            log.error(
                "[랭킹] ZSET 갱신 실패 — DB 집계에는 영향 없음 [dateKey={}, hourKey={}]",
                dateKey,
                hourKey,
                e,
            )
        }
    }

    /**
     * 가중치 조회. Redis 실패 또는 값 없음 시 DEFAULT로 fallback.
     */
    private fun resolveWeights(): RankingWeights {
        return try {
            rankingWeightsRepository.findCurrent() ?: RankingWeights.DEFAULT
        } catch (e: Exception) {
            log.warn("[랭킹] 가중치 조회 실패 — DEFAULT로 fallback", e)
            RankingWeights.DEFAULT
        }
    }
}
