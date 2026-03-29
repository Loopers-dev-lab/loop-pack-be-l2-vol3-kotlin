package com.loopers.domain.metrics

import com.loopers.domain.metrics.model.ProductMetrics
import com.loopers.support.error.CoreException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ProductMetricsTest {

    @Nested
    @DisplayName("ProductMetrics 생성 시")
    inner class Create {

        @Test
        fun `productId가 양수이면 생성된다`() {
            val metrics = ProductMetrics(productId = 1L)

            assertThat(metrics.productId).isEqualTo(1L)
            assertThat(metrics.viewCount).isEqualTo(0)
            assertThat(metrics.likeCount).isEqualTo(0)
            assertThat(metrics.salesCount).isEqualTo(0)
        }

        @Test
        fun `productId가 0이면 예외가 발생한다`() {
            assertThatThrownBy { ProductMetrics(productId = 0) }
                .isInstanceOf(CoreException::class.java)
        }

        @Test
        fun `productId가 음수이면 예외가 발생한다`() {
            assertThatThrownBy { ProductMetrics(productId = -1) }
                .isInstanceOf(CoreException::class.java)
        }
    }

    @Nested
    @DisplayName("조회수 증가 시")
    inner class IncrementViewCount {

        @Test
        fun `viewCount가 1 증가한다`() {
            val metrics = ProductMetrics(productId = 1L)

            metrics.incrementViewCount()

            assertThat(metrics.viewCount).isEqualTo(1)
        }
    }

    @Nested
    @DisplayName("좋아요 증감 시")
    inner class LikeCount {

        @Test
        fun `incrementLikeCount로 1 증가한다`() {
            val metrics = ProductMetrics(productId = 1L)

            metrics.incrementLikeCount()

            assertThat(metrics.likeCount).isEqualTo(1)
        }

        @Test
        fun `decrementLikeCount로 1 감소한다`() {
            val metrics = ProductMetrics(productId = 1L, likeCount = 5L)

            metrics.decrementLikeCount()

            assertThat(metrics.likeCount).isEqualTo(4)
        }

        @Test
        fun `likeCount가 0이면 더 이상 감소하지 않는다`() {
            val metrics = ProductMetrics(productId = 1L, likeCount = 0L)

            metrics.decrementLikeCount()

            assertThat(metrics.likeCount).isEqualTo(0)
        }
    }

    @Nested
    @DisplayName("판매량 증가 시")
    inner class IncrementSalesCount {

        @Test
        fun `기본 1씩 증가한다`() {
            val metrics = ProductMetrics(productId = 1L)

            metrics.incrementSalesCount()

            assertThat(metrics.salesCount).isEqualTo(1)
        }

        @Test
        fun `지정 수량만큼 증가한다`() {
            val metrics = ProductMetrics(productId = 1L)

            metrics.incrementSalesCount(3L)

            assertThat(metrics.salesCount).isEqualTo(3)
        }
    }
}
