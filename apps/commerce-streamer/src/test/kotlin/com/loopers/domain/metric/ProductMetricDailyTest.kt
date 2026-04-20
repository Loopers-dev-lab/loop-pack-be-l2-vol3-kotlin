package com.loopers.domain.metric

import com.loopers.domain.ranking.RankingScorePolicy
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Offset
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import kotlin.math.ln

@DisplayName("ProductMetricDaily")
class ProductMetricDailyTest {

    private val today = LocalDate.of(2026, 4, 16)

    @Nested
    @DisplayName("조회 이벤트 기록")
    inner class RecordView {

        @Test
        @DisplayName("view_count가 1 증가한다")
        fun recordView_incrementsViewCount() {
            val daily = ProductMetricDaily.register(productId = 1L, metricDate = today)

            val updated = daily.recordView()

            assertThat(updated.viewCount).isEqualTo(1)
        }

        @Test
        @DisplayName("연속 호출 시 누적된다")
        fun recordView_accumulates() {
            val daily = ProductMetricDaily.register(productId = 1L, metricDate = today)

            val updated = daily.recordView().recordView().recordView()

            assertThat(updated.viewCount).isEqualTo(3)
        }
    }

    @Nested
    @DisplayName("좋아요 등록 이벤트 기록")
    inner class RecordLike {

        @Test
        @DisplayName("like_count가 1 증가한다")
        fun recordLike_incrementsLikeCount() {
            val daily = ProductMetricDaily.register(productId = 1L, metricDate = today)

            val updated = daily.recordLike()

            assertThat(updated.likeCount).isEqualTo(1)
        }
    }

    @Nested
    @DisplayName("주문 이벤트 기록")
    inner class RecordOrder {

        @Test
        @DisplayName("units_sold, sales_amount, order_score가 누적된다")
        fun recordOrder_accumulatesAll() {
            val daily = ProductMetricDaily.register(productId = 1L, metricDate = today)
            val amount = 10_000L

            val updated = daily.recordOrder(quantity = 2, amount = amount)

            val expectedScore = RankingScorePolicy.ORDER_WEIGHT * ln(amount.toDouble())
            assertAll(
                { assertThat(updated.unitsSold).isEqualTo(2) },
                { assertThat(updated.salesAmount).isEqualTo(10_000L) },
                { assertThat(updated.orderScore).isCloseTo(expectedScore, Offset.offset(0.0001)) },
            )
        }

        @Test
        @DisplayName("amount가 0이면 order_score는 변하지 않지만 units_sold/sales_amount는 변한다")
        fun recordOrder_zeroAmount() {
            val daily = ProductMetricDaily.register(productId = 1L, metricDate = today)

            val updated = daily.recordOrder(quantity = 1, amount = 0L)

            assertAll(
                { assertThat(updated.unitsSold).isEqualTo(1) },
                { assertThat(updated.salesAmount).isEqualTo(0L) },
                { assertThat(updated.orderScore).isEqualTo(0.0) },
            )
        }

        @Test
        @DisplayName("quantity가 음수이면 예외를 던진다")
        fun recordOrder_negativeQuantity() {
            val daily = ProductMetricDaily.register(productId = 1L, metricDate = today)

            assertThrows<IllegalArgumentException> {
                daily.recordOrder(quantity = -1, amount = 10_000L)
            }
        }

        @Test
        @DisplayName("amount가 음수이면 예외를 던진다")
        fun recordOrder_negativeAmount() {
            val daily = ProductMetricDaily.register(productId = 1L, metricDate = today)

            assertThrows<IllegalArgumentException> {
                daily.recordOrder(quantity = 1, amount = -10_000L)
            }
        }

        @Test
        @DisplayName("여러 주문이 누적된다")
        fun recordOrder_multipleOrders() {
            val daily = ProductMetricDaily.register(productId = 1L, metricDate = today)

            val updated = daily
                .recordOrder(quantity = 1, amount = 10_000L)
                .recordOrder(quantity = 3, amount = 50_000L)

            val expectedScore = RankingScorePolicy.ORDER_WEIGHT * ln(10_000.0) +
                RankingScorePolicy.ORDER_WEIGHT * ln(50_000.0)
            assertAll(
                { assertThat(updated.unitsSold).isEqualTo(4) },
                { assertThat(updated.salesAmount).isEqualTo(60_000L) },
                { assertThat(updated.orderScore).isCloseTo(expectedScore, Offset.offset(0.0001)) },
            )
        }
    }

    @Nested
    @DisplayName("팩토리 메서드")
    inner class Factory {

        @Test
        @DisplayName("register 시 모든 카운터가 0이다")
        fun register_allZeros() {
            val daily = ProductMetricDaily.register(productId = 1L, metricDate = today)

            assertAll(
                { assertThat(daily.productId).isEqualTo(1L) },
                { assertThat(daily.metricDate).isEqualTo(today) },
                { assertThat(daily.viewCount).isEqualTo(0) },
                { assertThat(daily.likeCount).isEqualTo(0) },
                { assertThat(daily.unitsSold).isEqualTo(0) },
                { assertThat(daily.salesAmount).isEqualTo(0L) },
                { assertThat(daily.orderScore).isEqualTo(0.0) },
            )
        }

        @Test
        @DisplayName("retrieve 시 필드 값이 그대로 복원된다")
        fun retrieve_preservesFields() {
            val daily = ProductMetricDaily.retrieve(
                productId = 7L,
                metricDate = today,
                viewCount = 10,
                likeCount = 3,
                unitsSold = 4,
                salesAmount = 30_000L,
                orderScore = 12.5,
            )

            assertAll(
                { assertThat(daily.productId).isEqualTo(7L) },
                { assertThat(daily.metricDate).isEqualTo(today) },
                { assertThat(daily.viewCount).isEqualTo(10) },
                { assertThat(daily.likeCount).isEqualTo(3) },
                { assertThat(daily.unitsSold).isEqualTo(4) },
                { assertThat(daily.salesAmount).isEqualTo(30_000L) },
                { assertThat(daily.orderScore).isEqualTo(12.5) },
            )
        }

        @Test
        @DisplayName("retrieve 시 음수 필드가 있으면 예외를 던진다")
        fun retrieve_rejectsNegativeFields() {
            assertThrows<IllegalArgumentException> {
                ProductMetricDaily.retrieve(
                    productId = 1L,
                    metricDate = today,
                    viewCount = -1,
                    likeCount = 0,
                    unitsSold = 0,
                    salesAmount = 0L,
                    orderScore = 0.0,
                )
            }
        }
    }
}
