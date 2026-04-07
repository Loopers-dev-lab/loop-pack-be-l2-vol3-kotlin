package com.loopers.config.ranking

import com.loopers.domain.ranking.DailyAccumulationStrategy
import com.loopers.domain.ranking.SlidingWindowStrategy
import kotlin.math.pow
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertIs

@DisplayName("RankingConfiguration 단위 테스트")
class RankingConfigurationUnitTest {

    private val config = RankingConfiguration()

    @Test
    @DisplayName("daily 전략 선택")
    fun dailyStrategySelection() {
        val properties = RankingProperties().apply {
            strategy = "daily"
        }
        val strategy = config.rankingScoreStrategy(properties)
        assertIs<DailyAccumulationStrategy>(strategy)
    }

    @Test
    @DisplayName("sliding-window 전략 선택")
    fun slidingWindowStrategySelection() {
        val properties = RankingProperties().apply {
            strategy = "sliding-window"
        }
        val strategy = config.rankingScoreStrategy(properties)
        assertIs<SlidingWindowStrategy>(strategy)
    }

    @Test
    @DisplayName("커스텀 sliding-window 설정 적용")
    fun customSlidingWindowProperties() {
        val properties = RankingProperties().apply {
            strategy = "sliding-window"
            slidingWindow = RankingProperties.SlidingWindowProperties().apply {
                windowDays = 14
                decayFactor = 0.95
            }
        }
        val strategy = config.rankingScoreStrategy(properties) as SlidingWindowStrategy
        val score = strategy.calculateViewScore(daysAgo = 14)
        val expected = 0.1 * 0.95.pow(14)
        assertEquals(expected, score, 1e-6)
    }

    @Test
    @DisplayName("알 수 없는 전략은 예외 발생")
    fun unknownStrategyThrowsException() {
        val properties = RankingProperties().apply {
            strategy = "unknown-strategy"
        }
        assertThrows<IllegalArgumentException> {
            config.rankingScoreStrategy(properties)
        }
    }

    @Test
    @DisplayName("대소문자 무시")
    fun caseInsensitiveStrategy() {
        val properties = RankingProperties().apply {
            strategy = "SLIDING-WINDOW"
        }
        val strategy = config.rankingScoreStrategy(properties)
        assertIs<SlidingWindowStrategy>(strategy)
    }
}
