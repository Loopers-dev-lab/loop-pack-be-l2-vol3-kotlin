package com.loopers.domain.ranking

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class RankingScorePolicyTest {

    private val policy = RankingScorePolicy()

    @DisplayName("조회 점수를 계산할 때,")
    @Nested
    inner class CalculateViewScore {

        @DisplayName("가중치 0.1이 적용된 점수를 반환한다.")
        @Test
        fun returnsWeightedScore_whenViewEvent() {
            // act
            val score = policy.calculateViewScore()

            // assert
            assertThat(score).isEqualTo(0.1)
        }
    }

    @DisplayName("좋아요 점수를 계산할 때,")
    @Nested
    inner class CalculateLikeScore {

        @DisplayName("가중치 0.2가 적용된 점수를 반환한다.")
        @Test
        fun returnsWeightedScore_whenLikeEvent() {
            // act
            val score = policy.calculateLikeScore()

            // assert
            assertThat(score).isEqualTo(0.2)
        }
    }

    @DisplayName("주문 점수를 계산할 때,")
    @Nested
    inner class CalculateOrderScore {

        @DisplayName("금액에 가중치 0.6이 적용된 점수를 반환한다.")
        @Test
        fun returnsAmountWeightedScore_whenOrderEvent() {
            // arrange
            val amount = BigDecimal("10000")

            // act
            val score = policy.calculateOrderScore(amount)

            // assert
            assertThat(score).isEqualTo(6000.0)
        }

        @DisplayName("금액이 0이면, 0점을 반환한다.")
        @Test
        fun returnsZero_whenAmountIsZero() {
            // act
            val score = policy.calculateOrderScore(BigDecimal.ZERO)

            // assert
            assertThat(score).isEqualTo(0.0)
        }

        @DisplayName("소수점 금액도 정확히 계산한다.")
        @Test
        fun calculatesCorrectly_whenDecimalAmount() {
            // arrange
            val amount = BigDecimal("1500.50")

            // act
            val score = policy.calculateOrderScore(amount)

            // assert
            assertThat(score).isEqualTo(900.3)
        }
    }
}
