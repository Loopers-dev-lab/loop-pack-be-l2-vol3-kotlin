package com.loopers.batch.job.ranking

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class MetricsAggregationProcessorTest {

    private lateinit var processor: MetricsAggregationProcessor

    @BeforeEach
    fun setUp() {
        processor = MetricsAggregationProcessor()
    }

    @DisplayName("점수 계산: view×0.1 + like×0.2 + sales×0.7")
    @Test
    fun shouldCalculateScoreCorrectly() {
        val row = ProductMetricsDailyRow(productId = 1L, viewCount = 100L, salesCount = 100L, likeCount = 100L)

        val result = processor.process(row)

        // 100*0.1 + 100*0.2 + 100*0.7 = 100.0
        assertThat(result.score).isEqualTo(100.0)
    }

    @DisplayName("가중치 우선순위: sales(0.7) > like(0.2) > view(0.1)")
    @Test
    fun shouldApplyWeightsInCorrectOrder() {
        val viewOnly = processor.process(ProductMetricsDailyRow(1L, viewCount = 100L, salesCount = 0L, likeCount = 0L))
        val likeOnly = processor.process(ProductMetricsDailyRow(2L, viewCount = 0L, salesCount = 0L, likeCount = 100L))
        val salesOnly = processor.process(ProductMetricsDailyRow(3L, viewCount = 0L, salesCount = 100L, likeCount = 0L))

        assertThat(salesOnly.score).isEqualTo(70.0)
        assertThat(likeOnly.score).isEqualTo(20.0)
        assertThat(viewOnly.score).isEqualTo(10.0)
        assertThat(salesOnly.score).isGreaterThan(likeOnly.score)
        assertThat(likeOnly.score).isGreaterThan(viewOnly.score)
    }

    @DisplayName("productId는 원본 그대로 유지")
    @Test
    fun shouldPreserveProductId() {
        val row = ProductMetricsDailyRow(productId = 42L, viewCount = 0L, salesCount = 0L, likeCount = 0L)

        val result = processor.process(row)

        assertThat(result.productId).isEqualTo(42L)
    }

    @DisplayName("Processor는 랭크를 부여하지 않음 - score 기여값만 반환")
    @Test
    fun shouldReturnScoreContributionOnly() {
        val row = ProductMetricsDailyRow(productId = 1L, viewCount = 10L, salesCount = 5L, likeCount = 3L)

        val result = processor.process(row)

        assertThat(result).isInstanceOf(RankingScoreContribution::class.java)
        // 랭크 필드 없음 - Tasklet이 별도 처리
    }
}
