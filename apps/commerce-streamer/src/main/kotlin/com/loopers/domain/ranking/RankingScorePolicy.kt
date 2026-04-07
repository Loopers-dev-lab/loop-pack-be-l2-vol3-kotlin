package com.loopers.domain.ranking

import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
class RankingScorePolicy {

    fun calculateViewScore(): Double = VIEW_BASE_SCORE * VIEW_WEIGHT

    fun calculateLikeScore(): Double = LIKE_BASE_SCORE * LIKE_WEIGHT

    fun calculateOrderScore(amount: BigDecimal): Double = amount.toDouble() * ORDER_WEIGHT

    companion object {
        const val VIEW_WEIGHT = 0.1
        const val LIKE_WEIGHT = 0.2
        const val ORDER_WEIGHT = 0.6

        private const val VIEW_BASE_SCORE = 1.0
        private const val LIKE_BASE_SCORE = 1.0
    }
}
