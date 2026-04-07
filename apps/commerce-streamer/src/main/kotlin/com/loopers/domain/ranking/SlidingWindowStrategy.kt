package com.loopers.domain.ranking

import kotlin.math.pow

/**
 * 슬라이딩 윈도우 기반 랭킹 점수 전략
 *
 * 특징:
 * - 최근 N일 범위의 데이터만 고려
 * - 오래된 데이터는 decay factor 적용으로 점수 감소
 * - 콜드스타트 문제 해결: 신상품도 최근 활동만 반영되므로 빠르게 랭킹 진입
 *
 * 예시 (decay factor = 0.9):
 * - 오늘(daysAgo=0): score × 0.9^0 = score × 1.0
 * - 1일전(daysAgo=1): score × 0.9^1 = score × 0.9
 * - 2일전(daysAgo=2): score × 0.9^2 = score × 0.81
 * - 7일전(daysAgo=7): score × 0.9^7 = score × 0.478
 */
class SlidingWindowStrategy(
    private val windowDays: Int = 7,
    private val decayFactor: Double = 0.9,
) : RankingScoreStrategy {
    companion object {
        private const val VIEW_SCORE = 0.1
        private const val LIKE_SCORE = 0.2
        private const val ORDER_SCORE_PER_QUANTITY = 0.7
    }

    init {
        require(windowDays > 0) { "windowDays must be positive" }
        require(decayFactor in 0.0..1.0) { "decayFactor must be between 0.0 and 1.0" }
    }

    override fun calculateViewScore(daysAgo: Int): Double {
        return applyDecay(VIEW_SCORE, daysAgo)
    }

    override fun calculateLikeScore(increment: Boolean, daysAgo: Int): Double {
        val baseScore = if (increment) LIKE_SCORE else -LIKE_SCORE
        return applyDecay(baseScore, daysAgo)
    }

    override fun calculateOrderScore(quantity: Int, daysAgo: Int): Double {
        val baseScore = quantity * ORDER_SCORE_PER_QUANTITY
        return applyDecay(baseScore, daysAgo)
    }

    override fun getWindowDays(): Int = windowDays

    private fun applyDecay(score: Double, daysAgo: Int): Double {
        if (daysAgo > windowDays) {
            return 0.0
        }
        val decayedWeight = decayFactor.pow(daysAgo.toDouble())
        return score * decayedWeight
    }
}
