package com.loopers.domain.ranking

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.math.pow
import kotlin.test.assertEquals

@DisplayName("SlidingWindowStrategy 단위 테스트")
class SlidingWindowStrategyTest {

    private val strategy = SlidingWindowStrategy(windowDays = 7, decayFactor = 0.9)

    @Test
    @DisplayName("base score는 DailyAccumulationStrategy와 동일하다")
    fun baseScoresMatchDailyStrategy() {
        assertEquals(0.1, strategy.calculateViewScore())
        assertEquals(0.2, strategy.calculateLikeScore(increment = true))
        assertEquals(-0.2, strategy.calculateLikeScore(increment = false))
        assertEquals(0.7, strategy.calculateOrderScore(quantity = 1), 1e-6)
        assertEquals(2.1, strategy.calculateOrderScore(quantity = 3), 1e-6)
    }

    @Test
    @DisplayName("당일(daysAgo=0) decay weight는 1.0")
    fun decayWeightTodayIsOne() {
        assertEquals(1.0, strategy.getDecayWeight(daysAgo = 0))
    }

    @Test
    @DisplayName("1일전(daysAgo=1) decay weight는 0.9")
    fun decayWeightAfterOneDay() {
        assertEquals(0.9.pow(1), strategy.getDecayWeight(daysAgo = 1), 1e-6)
    }

    @Test
    @DisplayName("7일전(daysAgo=7) decay weight는 0.9^7")
    fun decayWeightAfterSevenDays() {
        assertEquals(0.9.pow(7), strategy.getDecayWeight(daysAgo = 7), 1e-6)
    }

    @Test
    @DisplayName("윈도우 범위를 초과(daysAgo > 7)하면 decay weight 0.0")
    fun decayWeightZeroBeyondWindow() {
        assertEquals(0.0, strategy.getDecayWeight(daysAgo = 8))
    }

    @Test
    @DisplayName("윈도우 범위 반환")
    fun returnsCorrectWindowDays() {
        assertEquals(7, strategy.getWindowDays())
    }

    @Test
    @DisplayName("windowDays 0 이하는 불가")
    fun rejectsNonPositiveWindowDays() {
        assertThrows<IllegalArgumentException> {
            SlidingWindowStrategy(windowDays = 0)
        }
    }

    @Test
    @DisplayName("decay factor 범위 [0.0, 1.0] 검증")
    fun rejectsInvalidDecayFactor() {
        assertThrows<IllegalArgumentException> {
            SlidingWindowStrategy(decayFactor = 1.5)
        }
        assertThrows<IllegalArgumentException> {
            SlidingWindowStrategy(decayFactor = -0.1)
        }
    }

    @Test
    @DisplayName("커스텀 windowDays와 decayFactor")
    fun supportCustomWindowAndDecay() {
        val custom = SlidingWindowStrategy(windowDays = 14, decayFactor = 0.95)
        assertEquals(14, custom.getWindowDays())
        assertEquals(0.95.pow(14), custom.getDecayWeight(daysAgo = 14), 1e-6)
        assertEquals(0.0, custom.getDecayWeight(daysAgo = 15))
    }
}
