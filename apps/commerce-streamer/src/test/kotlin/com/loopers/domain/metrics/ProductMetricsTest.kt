package com.loopers.domain.metrics

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("ProductMetrics")
class ProductMetricsTest {

    @DisplayName("ProductMetrics를 생성할 때,")
    @Nested
    inner class Create {

        @DisplayName("모든 집계 값이 0으로 초기화된다.")
        @Test
        fun initializesWithZeroCounts() {
            // act
            val metrics = ProductMetrics(productId = 100L)

            // assert
            assertThat(metrics.likeCount).isZero()
            assertThat(metrics.salesCount).isZero()
            assertThat(metrics.viewCount).isZero()
            assertThat(metrics.version).isZero()
        }
    }
}
