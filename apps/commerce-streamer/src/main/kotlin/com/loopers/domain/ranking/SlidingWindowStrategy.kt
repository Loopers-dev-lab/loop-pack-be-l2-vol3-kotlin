package com.loopers.domain.ranking

import kotlin.math.pow

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

    override fun calculateViewScore(): Double = VIEW_SCORE

    override fun calculateLikeScore(increment: Boolean): Double {
        return if (increment) LIKE_SCORE else -LIKE_SCORE
    }

    override fun calculateOrderScore(quantity: Int): Double {
        return quantity * ORDER_SCORE_PER_QUANTITY
    }

    override fun getWindowDays(): Int = windowDays

    override fun getDecayWeight(daysAgo: Int): Double {
        if (daysAgo > windowDays) return 0.0
        return decayFactor.pow(daysAgo.toDouble())
    }
}
