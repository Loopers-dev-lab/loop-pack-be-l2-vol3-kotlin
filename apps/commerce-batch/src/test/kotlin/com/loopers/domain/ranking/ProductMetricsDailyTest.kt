package com.loopers.domain.ranking

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import java.time.LocalDate

class ProductMetricsDailyTest {

    @Test
    fun `create는 productId+metricDate 복합키와 초기값을 그대로 설정한다`() {
        val daily = ProductMetricsDaily.create(
            productId = 42L,
            metricDate = LocalDate.of(2026, 4, 16),
            viewCount = 10,
            likeCount = 3,
            orderCount = 1,
            totalScore = 7.5,
            rankPosition = 5,
        )

        assertAll(
            { assertThat(daily.productId).isEqualTo(42L) },
            { assertThat(daily.metricDate).isEqualTo(LocalDate.of(2026, 4, 16)) },
            { assertThat(daily.viewCount).isEqualTo(10L) },
            { assertThat(daily.likeCount).isEqualTo(3L) },
            { assertThat(daily.orderCount).isEqualTo(1L) },
            { assertThat(daily.totalScore).isEqualTo(7.5) },
            { assertThat(daily.rankPosition).isEqualTo(5) },
        )
    }

    @Test
    fun `같은 productId+metricDate면 복합키가 동등하다`() {
        val a = ProductMetricsDailyId(1L, LocalDate.of(2026, 4, 16))
        val b = ProductMetricsDailyId(1L, LocalDate.of(2026, 4, 16))

        assertThat(a).isEqualTo(b)
    }
}
