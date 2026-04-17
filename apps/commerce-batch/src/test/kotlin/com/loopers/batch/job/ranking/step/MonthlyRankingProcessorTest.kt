package com.loopers.batch.job.ranking.step

import com.loopers.batch.job.ranking.MonthlyAggregationRow
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDate

@DisplayName("MonthlyRankingProcessor — Reader row 를 월간 MV 엔티티로 변환")
class MonthlyRankingProcessorTest {

    @DisplayName("첫 번째 호출은 rank_position = 1 을 부여한다")
    @Test
    fun firstCallAssignsRankOne() {
        val processor = MonthlyRankingProcessor(requestDate = "20260415")

        val result = processor.process(row(productId = 101L, score = 99.9))!!

        assertThat(result.rankPosition).isEqualTo(1)
        assertThat(result.productId).isEqualTo(101L)
        assertThat(result.score).isEqualTo(99.9)
    }

    @DisplayName("연속 호출 시 rank_position 이 1씩 증가한다")
    @Test
    fun consecutiveCallsIncrementRank() {
        val processor = MonthlyRankingProcessor(requestDate = "20260415")

        val first = processor.process(row(1L, 5.0))!!
        val second = processor.process(row(2L, 4.0))!!
        val third = processor.process(row(3L, 3.0))!!

        assertThat(first.rankPosition).isEqualTo(1)
        assertThat(second.rankPosition).isEqualTo(2)
        assertThat(third.rankPosition).isEqualTo(3)
    }

    @DisplayName("requestDate 가 어느 날이든 그 달의 1일~말일이 설정된다")
    @Test
    fun periodIsComputedFromRequestDate() {
        // 2026-04-15 (중순) → 해당 월은 2026-04-01 ~ 2026-04-30
        val processor = MonthlyRankingProcessor(requestDate = "20260415")

        val result = processor.process(row(1L, 1.0))!!

        assertThat(result.yearMonthVal).isEqualTo("2026-04")
        assertThat(result.periodStart).isEqualTo(LocalDate.of(2026, 4, 1))
        assertThat(result.periodEnd).isEqualTo(LocalDate.of(2026, 4, 30))
    }

    @DisplayName("윤년 2월 날짜 입력 시 29일까지의 기간이 설정된다")
    @Test
    fun leapFebruaryProducesFebruary29End() {
        // 2024-02-10 → 2024-02-01 ~ 2024-02-29
        val processor = MonthlyRankingProcessor(requestDate = "20240210")

        val result = processor.process(row(1L, 1.0))!!

        assertThat(result.yearMonthVal).isEqualTo("2024-02")
        assertThat(result.periodEnd).isEqualTo(LocalDate.of(2024, 2, 29))
    }

    @DisplayName("aggregation 수치들이 MV 엔티티에 그대로 전달된다")
    @Test
    fun aggregationCountsArePassedThrough() {
        val processor = MonthlyRankingProcessor(requestDate = "20260415")
        val row = MonthlyAggregationRow(
            productId = 50L,
            totalLikes = 10,
            totalViews = 20,
            totalSales = 3,
            score = 5.5,
        )

        val result = processor.process(row)!!

        assertThat(result.likesCount).isEqualTo(10)
        assertThat(result.viewsCount).isEqualTo(20)
        assertThat(result.salesCount).isEqualTo(3)
        assertThat(result.score).isEqualTo(5.5)
    }

    private fun row(productId: Long, score: Double) = MonthlyAggregationRow(
        productId = productId,
        totalLikes = 0,
        totalViews = 0,
        totalSales = 0,
        score = score,
    )
}
