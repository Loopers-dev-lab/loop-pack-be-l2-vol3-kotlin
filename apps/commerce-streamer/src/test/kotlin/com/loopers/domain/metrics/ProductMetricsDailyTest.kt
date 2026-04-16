package com.loopers.domain.metrics

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

@DisplayName("ProductMetricsDaily")
class ProductMetricsDailyTest {

    @DisplayName("ProductMetricsDaily를 생성할 때,")
    @Nested
    inner class Create {

        @DisplayName("상품 ID와 날짜로 생성하면 모든 집계 값이 0으로 초기화된다.")
        @Test
        fun initializesWithZeroCounts() {
            // arrange
            val productId = 100L
            val metricDate = LocalDate.of(2026, 4, 15)

            // act
            val metrics = ProductMetricsDaily(productId = productId, metricDate = metricDate)

            // assert
            assertThat(metrics.productId).isEqualTo(productId)
            assertThat(metrics.metricDate).isEqualTo(metricDate)
            assertThat(metrics.viewCount).isZero()
            assertThat(metrics.likeCount).isZero()
            assertThat(metrics.salesCount).isZero()
        }
    }
}
