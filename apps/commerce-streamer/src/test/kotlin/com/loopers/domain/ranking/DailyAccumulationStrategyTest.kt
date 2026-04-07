package com.loopers.domain.ranking

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

@DisplayName("DailyAccumulationStrategy 단위 테스트")
class DailyAccumulationStrategyTest {

    private val strategy = DailyAccumulationStrategy()

    @Test
    @DisplayName("조회 점수는 고정값 0.1")
    fun viewScoreIsConstant() {
        assertEquals(0.1, strategy.calculateViewScore(daysAgo = 0))
        assertEquals(0.1, strategy.calculateViewScore(daysAgo = 1))
        assertEquals(0.1, strategy.calculateViewScore(daysAgo = 100))
    }

    @Test
    @DisplayName("좋아요 증가는 0.2")
    fun likeIncrementScore() {
        assertEquals(0.2, strategy.calculateLikeScore(increment = true, daysAgo = 0))
    }

    @Test
    @DisplayName("좋아요 감소는 -0.2")
    fun likeDecrementScore() {
        assertEquals(-0.2, strategy.calculateLikeScore(increment = false, daysAgo = 0))
    }

    @Test
    @DisplayName("좋아요 점수는 daysAgo와 무관")
    fun likeScoreIgnoreDaysAgo() {
        assertEquals(0.2, strategy.calculateLikeScore(increment = true, daysAgo = 10))
        assertEquals(-0.2, strategy.calculateLikeScore(increment = false, daysAgo = 10))
    }

    @Test
    @DisplayName("주문 점수는 수량 × 0.7")
    fun orderScoreMultipliedByQuantity() {
        assertEquals(0.7, strategy.calculateOrderScore(quantity = 1, daysAgo = 0), 1e-6)
        assertEquals(2.1, strategy.calculateOrderScore(quantity = 3, daysAgo = 0), 1e-6)
        assertEquals(7.0, strategy.calculateOrderScore(quantity = 10, daysAgo = 0), 1e-6)
    }

    @Test
    @DisplayName("주문 점수는 daysAgo와 무관")
    fun orderScoreIgnoreDaysAgo() {
        assertEquals(0.7, strategy.calculateOrderScore(quantity = 1, daysAgo = 0))
        assertEquals(0.7, strategy.calculateOrderScore(quantity = 1, daysAgo = 100))
    }

    @Test
    @DisplayName("윈도우 범위는 0 (당일만)")
    fun windowDaysIsZero() {
        assertEquals(0, strategy.getWindowDays())
    }
}
