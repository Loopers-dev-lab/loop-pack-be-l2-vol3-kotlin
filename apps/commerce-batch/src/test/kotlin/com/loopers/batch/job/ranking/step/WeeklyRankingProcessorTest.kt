package com.loopers.batch.job.ranking.step

import com.loopers.batch.job.ranking.WeeklyAggregationRow
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("WeeklyRankingProcessor — Reader row 를 MV 엔티티로 변환")
class WeeklyRankingProcessorTest {

    @DisplayName("첫 번째 호출은 rank_position = 1 을 부여한다")
    @Test
    fun firstCallAssignsRankOne() {
        // arrange: requestDate 는 2026-04-15 (수) → 주간은 2026-04-13 ~ 2026-04-19
        val processor = WeeklyRankingProcessor(requestDate = "20260415")
        val row = row(productId = 101L, score = 99.9)

        // act
        val result = processor.process(row)

        // assert
        assertThat(result).isNotNull
        assertThat(result!!.rankPosition).isEqualTo(1)
        assertThat(result.productId).isEqualTo(101L)
        assertThat(result.score).isEqualTo(99.9)
    }

    @DisplayName("연속 호출 시 rank_position 이 1씩 증가한다")
    @Test
    fun consecutiveCallsIncrementRank() {
        val processor = WeeklyRankingProcessor(requestDate = "20260415")

        val first = processor.process(row(productId = 1L, score = 5.0))
        val second = processor.process(row(productId = 2L, score = 4.0))
        val third = processor.process(row(productId = 3L, score = 3.0))

        assertThat(first!!.rankPosition).isEqualTo(1)
        assertThat(second!!.rankPosition).isEqualTo(2)
        assertThat(third!!.rankPosition).isEqualTo(3)
    }

    @DisplayName("requestDate 가 어느 요일이든 그 주의 월~일로 periodStart/End 가 세팅된다")
    @Test
    fun periodIsComputedFromRequestDate() {
        // 2026-04-17 (금) 기준 주간은 2026-04-13 (월) ~ 2026-04-19 (일)
        val processor = WeeklyRankingProcessor(requestDate = "20260417")

        val result = processor.process(row(productId = 1L, score = 1.0))!!

        assertThat(result.periodStart).isEqualTo(java.time.LocalDate.of(2026, 4, 13))
        assertThat(result.periodEnd).isEqualTo(java.time.LocalDate.of(2026, 4, 19))
    }

    @DisplayName("aggregation 수치들이 MV 엔티티에 그대로 전달된다")
    @Test
    fun aggregationCountsArePassedThrough() {
        val processor = WeeklyRankingProcessor(requestDate = "20260415")
        val row = WeeklyAggregationRow(
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

    private fun row(productId: Long, score: Double) = WeeklyAggregationRow(
        productId = productId,
        totalLikes = 0,
        totalViews = 0,
        totalSales = 0,
        score = score,
    )
}
