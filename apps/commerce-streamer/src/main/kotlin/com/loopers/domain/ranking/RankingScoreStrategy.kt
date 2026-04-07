package com.loopers.domain.ranking

interface RankingScoreStrategy {
    fun calculateViewScore(): Double
    fun calculateLikeScore(increment: Boolean): Double
    fun calculateOrderScore(quantity: Int): Double
    fun getWindowDays(): Int
    fun getDecayWeight(daysAgo: Int): Double
}
