package com.loopers.batch.job.ranking

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * ProductMetricsDailyRow → RankingScoreContribution 변환 파이프라인 단위 테스트.
 * Reader는 raw row를 그대로 읽고, score 계산은 Processor에서 수행한다.
 */
class RankingMetricsReaderTest {

    @DisplayName("ProductMetricsDailyRow는 원시 daily 값을 그대로 보관")
    @Test
    fun shouldHoldRawDailyValues() {
        val row = ProductMetricsDailyRow(
            productId = 1L,
            viewCount = 50L,
            salesCount = 20L,
            likeCount = 10L,
        )

        assertThat(row.productId).isEqualTo(1L)
        assertThat(row.viewCount).isEqualTo(50L)
        assertThat(row.salesCount).isEqualTo(20L)
        assertThat(row.likeCount).isEqualTo(10L)
    }

    @DisplayName("RankingScoreContribution은 productId와 score만 포함")
    @Test
    fun shouldContainProductIdAndScore() {
        val contribution = RankingScoreContribution(productId = 5L, score = 42.5)

        assertThat(contribution.productId).isEqualTo(5L)
        assertThat(contribution.score).isEqualTo(42.5)
    }

    @DisplayName("Processor 파이프라인: raw row → score 기여값 (단일 일자 기준)")
    @Test
    fun shouldTransformRowToScoreContribution() {
        val processor = MetricsAggregationProcessor()
        // view=100, sales=10, like=50 → 100*0.1 + 50*0.2 + 10*0.7 = 10+10+7 = 27.0
        val row = ProductMetricsDailyRow(productId = 3L, viewCount = 100L, salesCount = 10L, likeCount = 50L)

        val result = processor.process(row)

        assertThat(result.productId).isEqualTo(3L)
        assertThat(result.score).isEqualTo(27.0)
    }

    // ===== Weekly Date Range Tests =====

    @DisplayName("[주간] 주어진 날짜에서 월요일~일요일 범위 계산")
    @Test
    fun weeklyRangeShouldReturnMondayToSunday() {
        // 2026-04-14 (화요일) → W16: 2026-04-13(월) ~ 2026-04-19(일)
        val date = LocalDate.of(2026, 4, 14)

        val (monday, sunday) = WeeklyRankingMetricsReader.getWeekRange(date)

        assertThat(monday.dayOfWeek).isEqualTo(DayOfWeek.MONDAY)
        assertThat(sunday.dayOfWeek).isEqualTo(DayOfWeek.SUNDAY)
        assertThat(monday).isEqualTo(LocalDate.of(2026, 4, 13))
        assertThat(sunday).isEqualTo(LocalDate.of(2026, 4, 19))
    }

    @DisplayName("[주간] 월요일이 입력되면 같은 주 월요일~일요일 반환")
    @Test
    fun weeklyRangeShouldWorkForMonday() {
        val monday = LocalDate.of(2026, 4, 13) // 월요일

        val (start, end) = WeeklyRankingMetricsReader.getWeekRange(monday)

        assertThat(start).isEqualTo(LocalDate.of(2026, 4, 13))
        assertThat(end).isEqualTo(LocalDate.of(2026, 4, 19))
    }

    @DisplayName("[주간] 일요일이 입력되면 같은 주 월요일~일요일 반환")
    @Test
    fun weeklyRangeShouldWorkForSunday() {
        val sunday = LocalDate.of(2026, 4, 19) // 일요일

        val (start, end) = WeeklyRankingMetricsReader.getWeekRange(sunday)

        assertThat(start).isEqualTo(LocalDate.of(2026, 4, 13))
        assertThat(end).isEqualTo(LocalDate.of(2026, 4, 19))
    }

    @DisplayName("[주간] 연말 경계: 12월 31일이 주간 범위를 다음 해로 넘김")
    @Test
    fun weeklyRangeShouldCrossYearBoundary() {
        val dec30 = LocalDate.of(2026, 12, 30) // 수요일

        val (start, end) = WeeklyRankingMetricsReader.getWeekRange(dec30)

        // 같은 주의 월요일은 12-28, 일요일은 01-03
        assertThat(start.year).isEqualTo(2026)
        assertThat(start.monthValue).isEqualTo(12)
        assertThat(start.dayOfMonth).isEqualTo(28)
        assertThat(end.year).isEqualTo(2027)
        assertThat(end.monthValue).isEqualTo(1)
        assertThat(end.dayOfMonth).isEqualTo(3)
    }

    // ===== Monthly Date Range Tests =====

    @DisplayName("[월간] 주어진 날짜에서 1일~말일 범위 계산")
    @Test
    fun monthlyRangeShouldReturnFirstToLastDay() {
        val date = LocalDate.of(2026, 4, 14)

        val (firstDay, lastDay) = MonthlyRankingMetricsReader.getMonthRange(date)

        assertThat(firstDay).isEqualTo(LocalDate.of(2026, 4, 1))
        assertThat(lastDay).isEqualTo(LocalDate.of(2026, 4, 30))
    }

    @DisplayName("[월간] 1월은 31일까지")
    @Test
    fun monthlyRangeShouldReturn31ForJanuary() {
        val date = LocalDate.of(2026, 1, 15)

        val (firstDay, lastDay) = MonthlyRankingMetricsReader.getMonthRange(date)

        assertThat(lastDay.dayOfMonth).isEqualTo(31)
    }

    @DisplayName("[월간] 2월은 평년 28일")
    @Test
    fun monthlyRangeShouldReturn28ForFebruaryNonLeapYear() {
        val date = LocalDate.of(2026, 2, 15)

        val (firstDay, lastDay) = MonthlyRankingMetricsReader.getMonthRange(date)

        assertThat(lastDay.dayOfMonth).isEqualTo(28)
    }

    @DisplayName("[월간] 2월 윤년 29일")
    @Test
    fun monthlyRangeShouldReturn29ForFebruaryLeapYear() {
        val date = LocalDate.of(2024, 2, 15) // 윤년

        val (firstDay, lastDay) = MonthlyRankingMetricsReader.getMonthRange(date)

        assertThat(lastDay.dayOfMonth).isEqualTo(29)
    }
}
