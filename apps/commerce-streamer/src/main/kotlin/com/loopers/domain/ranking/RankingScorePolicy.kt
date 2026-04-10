package com.loopers.domain.ranking

import java.math.BigDecimal

class RankingScorePolicy {

    fun calculateViewScore(weight: Double): Double = VIEW_BASE_SCORE * weight

    fun calculateLikeScore(weight: Double): Double = LIKE_BASE_SCORE * weight

    fun calculateOrderScore(amount: BigDecimal, weight: Double): Double = amount.toDouble() * weight

    companion object {
        private const val VIEW_BASE_SCORE = 1.0
        private const val LIKE_BASE_SCORE = 1.0
    }
}
