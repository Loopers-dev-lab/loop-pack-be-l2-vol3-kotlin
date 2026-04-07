package com.loopers.domain.ranking

class DailyAccumulationStrategy : RankingScoreStrategy {
    companion object {
        private const val VIEW_SCORE = 0.1
        private const val LIKE_SCORE = 0.2
        private const val ORDER_SCORE_PER_QUANTITY = 0.7
    }

    override fun calculateViewScore(): Double = VIEW_SCORE

    override fun calculateLikeScore(increment: Boolean): Double {
        return if (increment) LIKE_SCORE else -LIKE_SCORE
    }

    override fun calculateOrderScore(quantity: Int): Double {
        return quantity * ORDER_SCORE_PER_QUANTITY
    }

    override fun getWindowDays(): Int = 0

    override fun getDecayWeight(daysAgo: Int): Double = 1.0
}
