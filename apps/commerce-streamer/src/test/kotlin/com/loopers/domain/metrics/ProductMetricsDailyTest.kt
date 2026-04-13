package com.loopers.domain.metrics

import com.loopers.domain.metrics.model.ProductMetricsDaily
import com.loopers.support.error.CoreException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

class ProductMetricsDailyTest {

    private val today = LocalDate.of(2026, 4, 13)

    @Nested
    @DisplayName("ProductMetricsDaily 생성 시")
    inner class Create {

        @Test
        fun `productId가 양수이면 생성된다`() {
            val daily = ProductMetricsDaily(productId = 1L, metricDate = today)

            assertThat(daily.productId).isEqualTo(1L)
            assertThat(daily.metricDate).isEqualTo(today)
            assertThat(daily.viewCount).isEqualTo(0)
            assertThat(daily.likeCount).isEqualTo(0)
            assertThat(daily.salesCount).isEqualTo(0)
        }

        @Test
        fun `productId가 0이면 예외가 발생한다`() {
            assertThatThrownBy { ProductMetricsDaily(productId = 0, metricDate = today) }
                .isInstanceOf(CoreException::class.java)
        }

        @Test
        fun `productId가 음수이면 예외가 발생한다`() {
            assertThatThrownBy { ProductMetricsDaily(productId = -1, metricDate = today) }
                .isInstanceOf(CoreException::class.java)
        }
    }

    @Nested
    @DisplayName("조회수 증가 시")
    inner class IncrementViewCount {

        @Test
        fun `viewCount가 1 증가한다`() {
            val daily = ProductMetricsDaily(productId = 1L, metricDate = today)

            daily.incrementViewCount()

            assertThat(daily.viewCount).isEqualTo(1)
        }
    }

    @Nested
    @DisplayName("좋아요 증감 시")
    inner class LikeCount {

        @Test
        fun `incrementLikeCount로 1 증가한다`() {
            val daily = ProductMetricsDaily(productId = 1L, metricDate = today)

            daily.incrementLikeCount()

            assertThat(daily.likeCount).isEqualTo(1)
        }

        @Test
        fun `decrementLikeCount로 1 감소하고 true를 반환한다`() {
            val daily = ProductMetricsDaily(productId = 1L, metricDate = today, likeCount = 5L)

            val result = daily.decrementLikeCount()

            assertThat(result).isTrue()
            assertThat(daily.likeCount).isEqualTo(4)
        }

        @Test
        fun `likeCount가 0이면 감소하지 않고 false를 반환한다`() {
            val daily = ProductMetricsDaily(productId = 1L, metricDate = today, likeCount = 0L)

            val result = daily.decrementLikeCount()

            assertThat(result).isFalse()
            assertThat(daily.likeCount).isEqualTo(0)
        }
    }

    @Nested
    @DisplayName("판매량 증가 시")
    inner class IncrementSalesCount {

        @Test
        fun `지정 수량만큼 증가한다`() {
            val daily = ProductMetricsDaily(productId = 1L, metricDate = today)

            daily.incrementSalesCount(3L)

            assertThat(daily.salesCount).isEqualTo(3)
        }

        @Test
        fun `수량이 0이면 예외가 발생한다`() {
            val daily = ProductMetricsDaily(productId = 1L, metricDate = today)

            assertThatThrownBy { daily.incrementSalesCount(0) }
                .isInstanceOf(CoreException::class.java)
        }

        @Test
        fun `수량이 음수이면 예외가 발생한다`() {
            val daily = ProductMetricsDaily(productId = 1L, metricDate = today)

            assertThatThrownBy { daily.incrementSalesCount(-1) }
                .isInstanceOf(CoreException::class.java)
        }
    }
}
