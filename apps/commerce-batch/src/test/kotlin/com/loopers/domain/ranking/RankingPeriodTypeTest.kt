package com.loopers.domain.ranking

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

class RankingPeriodTypeTest {

    @Nested
    @DisplayName("WEEKLY 기간 계산")
    inner class WeeklyPeriod {

        @DisplayName("수요일 기준으로 해당 주 월요일을 반환한다")
        @Test
        fun periodStartDate_whenWednesday() {
            // arrange
            val wednesday = LocalDate.of(2026, 4, 15)

            // act
            val startDate = RankingPeriodType.WEEKLY.periodStartDate(wednesday)

            // assert
            assertThat(startDate).isEqualTo(LocalDate.of(2026, 4, 13))
        }

        @DisplayName("수요일 기준으로 해당 주 일요일을 반환한다")
        @Test
        fun periodEndDate_whenWednesday() {
            // arrange
            val wednesday = LocalDate.of(2026, 4, 15)

            // act
            val endDate = RankingPeriodType.WEEKLY.periodEndDate(wednesday)

            // assert
            assertThat(endDate).isEqualTo(LocalDate.of(2026, 4, 19))
        }

        @DisplayName("월요일 기준으로 시작일은 자기 자신이다")
        @Test
        fun periodStartDate_whenMonday() {
            // arrange
            val monday = LocalDate.of(2026, 4, 13)

            // act
            val startDate = RankingPeriodType.WEEKLY.periodStartDate(monday)

            // assert
            assertThat(startDate).isEqualTo(monday)
        }

        @DisplayName("일요일 기준으로 종료일은 자기 자신이다")
        @Test
        fun periodEndDate_whenSunday() {
            // arrange
            val sunday = LocalDate.of(2026, 4, 19)

            // act
            val endDate = RankingPeriodType.WEEKLY.periodEndDate(sunday)

            // assert
            assertThat(endDate).isEqualTo(sunday)
        }

        @DisplayName("월경계를 넘는 주에 대해 올바르게 계산한다")
        @Test
        fun periodDate_whenCrossMonthBoundary() {
            // arrange
            val thursday = LocalDate.of(2026, 4, 30)

            // act
            val startDate = RankingPeriodType.WEEKLY.periodStartDate(thursday)
            val endDate = RankingPeriodType.WEEKLY.periodEndDate(thursday)

            // assert
            assertThat(startDate).isEqualTo(LocalDate.of(2026, 4, 27))
            assertThat(endDate).isEqualTo(LocalDate.of(2026, 5, 3))
        }
    }

    @Nested
    @DisplayName("MONTHLY 기간 계산")
    inner class MonthlyPeriod {

        @DisplayName("월 중간일 기준으로 1일을 반환한다")
        @Test
        fun periodStartDate_whenMidMonth() {
            // arrange
            val midMonth = LocalDate.of(2026, 4, 15)

            // act
            val startDate = RankingPeriodType.MONTHLY.periodStartDate(midMonth)

            // assert
            assertThat(startDate).isEqualTo(LocalDate.of(2026, 4, 1))
        }

        @DisplayName("월 중간일 기준으로 말일을 반환한다")
        @Test
        fun periodEndDate_whenMidMonth() {
            // arrange
            val midMonth = LocalDate.of(2026, 4, 15)

            // act
            val endDate = RankingPeriodType.MONTHLY.periodEndDate(midMonth)

            // assert
            assertThat(endDate).isEqualTo(LocalDate.of(2026, 4, 30))
        }

        @DisplayName("2월 말일을 올바르게 계산한다")
        @Test
        fun periodEndDate_whenFebruary() {
            // arrange
            val feb = LocalDate.of(2026, 2, 10)

            // act
            val endDate = RankingPeriodType.MONTHLY.periodEndDate(feb)

            // assert
            assertThat(endDate).isEqualTo(LocalDate.of(2026, 2, 28))
        }

        @DisplayName("윤년 2월 말일을 올바르게 계산한다")
        @Test
        fun periodEndDate_whenLeapYearFebruary() {
            // arrange
            val feb = LocalDate.of(2028, 2, 10)

            // act
            val endDate = RankingPeriodType.MONTHLY.periodEndDate(feb)

            // assert
            assertThat(endDate).isEqualTo(LocalDate.of(2028, 2, 29))
        }

        @DisplayName("1일 기준으로 시작일은 자기 자신이다")
        @Test
        fun periodStartDate_whenFirstDay() {
            // arrange
            val firstDay = LocalDate.of(2026, 4, 1)

            // act
            val startDate = RankingPeriodType.MONTHLY.periodStartDate(firstDay)

            // assert
            assertThat(startDate).isEqualTo(firstDay)
        }
    }
}
