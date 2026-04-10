package com.loopers.domain.ranking

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Offset
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class RankingScoreCalculatorTest {

    @Nested
    @DisplayName("calculate")
    inner class Calculate {

        @Test
        @DisplayName("DEFAULT 가중치로 가중합산 점수를 정확하게 계산한다")
        fun calculatesWeightedScoreWithDefaultWeights() {
            // arrange
            val viewCount = 100L
            val likeCount = 50L
            val orderCount = 10L

            // act
            val score = RankingScoreCalculator.calculate(
                viewCount = viewCount,
                likeCount = likeCount,
                orderCount = orderCount,
                weights = RankingWeights.DEFAULT,
            )

            // assert — (100 * 0.1) + (50 * 0.2) + (10 * 0.7) = 10 + 10 + 7 = 27.0
            assertThat(score).isCloseTo(27.0, Offset.offset(0.001))
        }

        @Test
        @DisplayName("모든 값이 0이면 점수는 0.0이다")
        fun zeroCountsProduceZeroScore() {
            // act
            val score = RankingScoreCalculator.calculate(
                viewCount = 0,
                likeCount = 0,
                orderCount = 0,
                weights = RankingWeights.DEFAULT,
            )

            // assert
            assertThat(score).isEqualTo(0.0)
        }

        @Test
        @DisplayName("주문 가중치가 가장 높아 같은 count일 때 주문이 점수를 지배한다")
        fun orderWeightDominates() {
            // arrange
            val weights = RankingWeights.DEFAULT
            val viewOnly = RankingScoreCalculator.calculate(100, 0, 0, weights)
            val likeOnly = RankingScoreCalculator.calculate(0, 100, 0, weights)
            val orderOnly = RankingScoreCalculator.calculate(0, 0, 100, weights)

            // assert
            assertThat(orderOnly).isGreaterThan(likeOnly)
            assertThat(likeOnly).isGreaterThan(viewOnly)
        }

        @Test
        @DisplayName("가중치가 바뀌면 점수도 비례해서 바뀐다")
        fun customWeightsAreApplied() {
            // arrange — 주문 가중치를 대폭 낮춘 상황
            val customWeights = RankingWeights(view = 0.5, like = 0.3, order = 0.2)

            // act
            val score = RankingScoreCalculator.calculate(
                viewCount = 100,
                likeCount = 50,
                orderCount = 10,
                weights = customWeights,
            )

            // assert — (100 * 0.5) + (50 * 0.3) + (10 * 0.2) = 50 + 15 + 2 = 67.0
            assertThat(score).isCloseTo(67.0, Offset.offset(0.001))
        }

        @Test
        @DisplayName("가중치 검증 — 음수 값은 생성자에서 거부된다")
        fun negativeWeightIsRejected() {
            // assert
            org.junit.jupiter.api.assertThrows<IllegalArgumentException> {
                RankingWeights(view = -0.1, like = 0.2, order = 0.7)
            }
        }
    }
}
