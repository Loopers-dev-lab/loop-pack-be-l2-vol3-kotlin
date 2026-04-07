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
        assertEquals(0.1, strategy.calculateViewScore())
    }

    @Test
    @DisplayName("좋아요 증가는 0.2, 감소는 -0.2")
    fun likeScores() {
        assertEquals(0.2, strategy.calculateLikeScore(increment = true))
        assertEquals(-0.2, strategy.calculateLikeScore(increment = false))
    }

    @Test
    @DisplayName("주문 점수는 수량 × 0.7")
    fun orderScoreMultipliedByQuantity() {
        assertEquals(0.7, strategy.calculateOrderScore(quantity = 1), 1e-6)
        assertEquals(2.1, strategy.calculateOrderScore(quantity = 3), 1e-6)
        assertEquals(7.0, strategy.calculateOrderScore(quantity = 10), 1e-6)
    }

    @Test
    @DisplayName("윈도우 범위는 0 (당일만)")
    fun windowDaysIsZero() {
        assertEquals(0, strategy.getWindowDays())
    }

    @Test
    @DisplayName("decay weight는 항상 1.0 (감쇠 없음)")
    fun decayWeightAlwaysOne() {
        assertEquals(1.0, strategy.getDecayWeight(0))
        assertEquals(1.0, strategy.getDecayWeight(7))
        assertEquals(1.0, strategy.getDecayWeight(100))
    }
}
