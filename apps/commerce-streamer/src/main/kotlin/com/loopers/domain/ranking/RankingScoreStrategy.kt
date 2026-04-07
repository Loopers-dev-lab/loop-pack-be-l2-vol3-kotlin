package com.loopers.domain.ranking

interface RankingScoreStrategy {
    fun calculateViewScore(daysAgo: Int = 0): Double
    fun calculateLikeScore(increment: Boolean, daysAgo: Int = 0): Double
    fun calculateOrderScore(quantity: Int, daysAgo: Int = 0): Double

    /**
     * 슬라이딩 윈도우 범위 반환
     * DailyAccumulationStrategy는 0 (당일만), SlidingWindowStrategy는 7 (최근 7일) 등
     */
    fun getWindowDays(): Int
}
