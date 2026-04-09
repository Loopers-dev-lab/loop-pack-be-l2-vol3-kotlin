package com.loopers.domain.ranking

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("RankingScorePolicy")
class RankingScorePolicyTest {
    private val policy = RankingScorePolicy()

    @Nested
    @DisplayName("조회 시그널은 0.1 가중치를 반환한다")
    inner class ViewSignal {

        @Test
        @DisplayName("VIEW → 0.1")
        fun calculateIncrement_view() {
            val increment = policy.calculateIncrement(RankingSignalType.VIEW)

            assertThat(increment).isEqualTo(0.1)
        }
    }

    @Nested
    @DisplayName("좋아요 시그널은 0.2 가중치를 반환한다")
    inner class LikeSignal {

        @Test
        @DisplayName("LIKE → 0.2")
        fun calculateIncrement_like() {
            val increment = policy.calculateIncrement(RankingSignalType.LIKE)

            assertThat(increment).isEqualTo(0.2)
        }
    }

    @Nested
    @DisplayName("주문 시그널은 0.7 × quantity 가중치를 반환한다")
    inner class OrderSignal {

        @Test
        @DisplayName("ORDER, quantity=1 → 0.7")
        fun calculateIncrement_order_singleQuantity() {
            val increment = policy.calculateIncrement(RankingSignalType.ORDER, quantity = 1)

            assertThat(increment).isEqualTo(0.7)
        }

        @Test
        @DisplayName("ORDER, quantity=3 → 0.7 × 3")
        fun calculateIncrement_order_multipleQuantity() {
            val increment = policy.calculateIncrement(RankingSignalType.ORDER, quantity = 3)

            assertThat(increment).isCloseTo(2.1, org.assertj.core.data.Offset.offset(0.001))
        }
    }

    @Nested
    @DisplayName("가중치 순서가 의도대로 동작한다")
    inner class WeightOrdering {

        @Test
        @DisplayName("주문 1건(0.7) > 좋아요 3건(0.6)")
        fun weightOrdering_orderBeatsMultipleLikes() {
            val orderScore = policy.calculateIncrement(RankingSignalType.ORDER, quantity = 1)
            val likeScore = policy.calculateIncrement(RankingSignalType.LIKE) * 3

            assertThat(orderScore).isGreaterThan(likeScore)
        }
    }
}
