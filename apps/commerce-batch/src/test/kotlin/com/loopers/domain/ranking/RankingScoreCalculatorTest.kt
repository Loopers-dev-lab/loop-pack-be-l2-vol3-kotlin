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
        @DisplayName("기본 가중치로 스코어를 계산한다")
        fun calculatesWithDefaultWeights() {
            // arrange
            val viewCount = 100L
            val likeCount = 10L
            val orderCount = 5L

            // act
            val score = RankingScoreCalculator.calculate(
                viewCount = viewCount,
                likeCount = likeCount,
                orderCount = orderCount,
                viewWeight = 0.1,
                likeWeight = 0.2,
                orderWeight = 0.7,
            )

            // assert — (100*0.1) + (10*0.2) + (5*0.7) = 10 + 2 + 3.5 = 15.5
            assertThat(score).isCloseTo(15.5, Offset.offset(0.001))
        }

        @Test
        @DisplayName("모든 카운트가 0이면 스코어도 0이다")
        fun zeroCountsReturnZeroScore() {
            // act
            val score = RankingScoreCalculator.calculate(
                viewCount = 0L,
                likeCount = 0L,
                orderCount = 0L,
                viewWeight = 0.1,
                likeWeight = 0.2,
                orderWeight = 0.7,
            )

            // assert
            assertThat(score).isEqualTo(0.0)
        }

        @Test
        @DisplayName("주문 가중치가 높으면 주문 수가 스코어에 크게 반영된다")
        fun orderHeavyWeightDominates() {
            // arrange — 조회 1000건이지만 주문 0건 vs 조회 10건이지만 주문 100건
            val viewHeavy = RankingScoreCalculator.calculate(
                viewCount = 1000L, likeCount = 0L, orderCount = 0L,
                viewWeight = 0.1, likeWeight = 0.2, orderWeight = 0.7,
            )
            val orderHeavy = RankingScoreCalculator.calculate(
                viewCount = 10L, likeCount = 0L, orderCount = 100L,
                viewWeight = 0.1, likeWeight = 0.2, orderWeight = 0.7,
            )

            // assert — 주문 100건 (70.0 + 1.0 = 71.0) > 조회 1000건 (100.0)
            assertThat(viewHeavy).isCloseTo(100.0, Offset.offset(0.001))
            assertThat(orderHeavy).isCloseTo(71.0, Offset.offset(0.001))
        }

        @Test
        @DisplayName("커스텀 가중치가 정상 적용된다")
        fun customWeightsApplied() {
            // act
            val score = RankingScoreCalculator.calculate(
                viewCount = 100L,
                likeCount = 50L,
                orderCount = 10L,
                viewWeight = 0.5,
                likeWeight = 0.3,
                orderWeight = 0.2,
            )

            // assert — (100*0.5) + (50*0.3) + (10*0.2) = 50 + 15 + 2 = 67.0
            assertThat(score).isCloseTo(67.0, Offset.offset(0.001))
        }
    }
}
