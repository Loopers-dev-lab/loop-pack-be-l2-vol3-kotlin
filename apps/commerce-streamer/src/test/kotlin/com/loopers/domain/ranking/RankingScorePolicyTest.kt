package com.loopers.domain.ranking

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Offset
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("RankingScorePolicy")
class RankingScorePolicyTest {
    private val policy = RankingScorePolicy()

    @Nested
    @DisplayName("VIEW, LIKE score 계산은 변경되지 않는다")
    inner class ViewAndLikeUnchanged {

        @Test
        @DisplayName("VIEW → 0.1")
        fun calculateIncrement_view() {
            val increment = policy.calculateIncrement(RankingSignalType.VIEW)
            assertThat(increment).isEqualTo(0.1)
        }

        @Test
        @DisplayName("LIKE → 0.2")
        fun calculateIncrement_like() {
            val increment = policy.calculateIncrement(RankingSignalType.LIKE)
            assertThat(increment).isEqualTo(0.2)
        }
    }

    @Nested
    @DisplayName("주문 시그널은 ORDER_WEIGHT × ln(orderAmount)로 계산된다")
    inner class OrderSignalWithPrice {

        @Test
        @DisplayName("orderAmount=10000 → 0.7 × ln(10000)")
        fun calculateOrderIncrement_normalCase() {
            val increment = policy.calculateOrderIncrement(10000L)
            val expected = RankingScorePolicy.ORDER_WEIGHT * kotlin.math.ln(10000.0)
            assertThat(increment).isCloseTo(expected, Offset.offset(0.001))
        }

        @Test
        @DisplayName("orderAmount=15000 (5000×3) → 0.7 × ln(15000)")
        fun calculateOrderIncrement_multiQuantity() {
            val increment = policy.calculateOrderIncrement(15000L)
            val expected = RankingScorePolicy.ORDER_WEIGHT * kotlin.math.ln(15000.0)
            assertThat(increment).isCloseTo(expected, Offset.offset(0.001))
        }
    }

    @Nested
    @DisplayName("orderAmount ≤ 0이면 score는 0이다")
    inner class ZeroPriceHandling {

        @Test
        @DisplayName("orderAmount=0 → 0")
        fun calculateOrderIncrement_zeroAmount() {
            val increment = policy.calculateOrderIncrement(0L)
            assertThat(increment).isEqualTo(0.0)
        }

        @Test
        @DisplayName("orderAmount=-1 → 0")
        fun calculateOrderIncrement_negativeAmount() {
            val increment = policy.calculateOrderIncrement(-1L)
            assertThat(increment).isEqualTo(0.0)
        }
    }

    @Nested
    @DisplayName("고가 상품 1건보다 저가 상품 다수 주문이 더 높은 점수를 받을 수 있다")
    inner class LogNormalizationEffect {

        @Test
        @DisplayName("100,000원 1건 < 10,000원 20건 (log 정규화 효과)")
        fun logNormalization_cheapMultipleBeatsSingleExpensive() {
            val expensiveSingle = policy.calculateOrderIncrement(100_000L)
            val cheapMultiple = policy.calculateOrderIncrement(200_000L)
            assertThat(cheapMultiple).isGreaterThan(expensiveSingle)
        }
    }
}
