package com.loopers.config.ranking

import com.loopers.domain.ranking.DefaultScoringStrategy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertIs

@DisplayName("RankingConfiguration 단위 테스트")
class RankingConfigurationUnitTest {

    private val config = RankingConfiguration()

    @Test
    @DisplayName("default 점수 전략 선택")
    fun defaultScoringStrategySelection() {
        val properties = RankingProperties().apply {
            scoring = "default"
        }
        assertIs<DefaultScoringStrategy>(config.scoringStrategy(properties))
    }

    @Test
    @DisplayName("알 수 없는 점수 전략은 예외 발생")
    fun unknownScoringStrategyThrows() {
        val properties = RankingProperties().apply {
            scoring = "unknown"
        }
        assertThrows<IllegalArgumentException> {
            config.scoringStrategy(properties)
        }
    }

    @Test
    @DisplayName("대소문자 무시")
    fun caseInsensitive() {
        val properties = RankingProperties().apply {
            scoring = "DEFAULT"
        }
        assertIs<DefaultScoringStrategy>(config.scoringStrategy(properties))
    }
}
