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
    @DisplayName("당일(daysAgo=0) 조회 이벤트는 기본 점수 그대로")
    fun viewScoreTodayHasNoDecay() {
        val score = strategy.calculateViewScore(daysAgo = 0)
        assertEquals(0.1, score)
    }

    @Test
    @DisplayName("1일전(daysAgo=1) 조회는 decay factor 적용")
    fun viewScoreDecaysAfterOneDay() {
        val expected = 0.1 * 0.9.pow(1)
        val actual = strategy.calculateViewScore(daysAgo = 1)
        assertEquals(expected, actual, 1e-6)
    }

    @Test
    @DisplayName("7일전(daysAgo=7) 조회는 큰 decay 적용")
    fun viewScoreDecaysAfterSevenDays() {
        val expected = 0.1 * 0.9.pow(7)
        val actual = strategy.calculateViewScore(daysAgo = 7)
        assertEquals(expected, actual, 1e-6)
    }

    @Test
    @DisplayName("윈도우 범위를 초과(daysAgo > 7)하면 0점")
    fun scoreZeroWhenBeyondWindow() {
        val score = strategy.calculateViewScore(daysAgo = 8)
        assertEquals(0.0, score)
    }

    @Test
    @DisplayName("좋아요 증가는 양수, 감소는 음수")
    fun likeScoresApplyCorrectSign() {
        val incrementScore = strategy.calculateLikeScore(increment = true, daysAgo = 0)
        val decrementScore = strategy.calculateLikeScore(increment = false, daysAgo = 0)
        assertEquals(0.2, incrementScore)
        assertEquals(-0.2, decrementScore)
    }

    @Test
    @DisplayName("좋아요 감소도 decay 적용")
    fun likeDecayIsAppliedToDecrement() {
        val expected = 0.2 * 0.9.pow(3)
        val actual = strategy.calculateLikeScore(increment = false, daysAgo = 3)
        assertEquals(-expected, actual, 1e-6)
    }

    @Test
    @DisplayName("주문 점수는 수량에 따라 계산 후 decay 적용")
    fun orderScoreMultipliedByQuantityThenDecayed() {
        val quantity = 3
        val expected = (quantity * 0.7) * 0.9.pow(2)
        val actual = strategy.calculateOrderScore(quantity = quantity, daysAgo = 2)
        assertEquals(expected, actual, 1e-6)
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
    @DisplayName("사용자 정의 windowDays와 decayFactor")
    fun supportCustomWindowAndDecay() {
        val customStrategy = SlidingWindowStrategy(windowDays = 14, decayFactor = 0.95)
        assertEquals(14, customStrategy.getWindowDays())

        // daysAgo = 14는 여전히 window 내에 있으므로 decay 적용
        val expected = 0.1 * 0.95.pow(14)
        val score = customStrategy.calculateViewScore(daysAgo = 14)
        assertEquals(expected, score, 1e-6)

        // daysAgo = 15는 window 범위를 초과하므로 0
        val scoreOutside = customStrategy.calculateViewScore(daysAgo = 15)
        assertEquals(0.0, scoreOutside)
    }
}
