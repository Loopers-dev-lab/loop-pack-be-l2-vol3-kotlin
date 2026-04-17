package com.loopers.domain.mv

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDate

@DisplayName("MonthlyPeriod — 월 경계 계산")
class MonthlyPeriodTest {

    @DisplayName("월 중간 날짜를 입력하면 그 달 1일과 말일이 반환된다")
    @Test
    fun midMonthReturnsFirstAndLast() {
        val midMonth = LocalDate.of(2026, 4, 15)

        val period = MonthlyPeriod.of(midMonth)

        assertThat(period.yearMonthVal).isEqualTo("2026-04")
        assertThat(period.start).isEqualTo(LocalDate.of(2026, 4, 1))
        assertThat(period.end).isEqualTo(LocalDate.of(2026, 4, 30))
    }

    @DisplayName("1일을 입력해도 동일하게 1일~말일이 반환된다")
    @Test
    fun firstDayStillReturnsFirstAndLast() {
        val firstDay = LocalDate.of(2026, 4, 1)

        val period = MonthlyPeriod.of(firstDay)

        assertThat(period.start).isEqualTo(LocalDate.of(2026, 4, 1))
        assertThat(period.end).isEqualTo(LocalDate.of(2026, 4, 30))
    }

    @DisplayName("말일을 입력해도 동일하게 1일~말일이 반환된다")
    @Test
    fun lastDayStillReturnsFirstAndLast() {
        val lastDay = LocalDate.of(2026, 4, 30)

        val period = MonthlyPeriod.of(lastDay)

        assertThat(period.start).isEqualTo(LocalDate.of(2026, 4, 1))
        assertThat(period.end).isEqualTo(LocalDate.of(2026, 4, 30))
    }

    @DisplayName("윤년 2월은 29일까지 반환된다 (2024년)")
    @Test
    fun leapFebruaryEndsOn29th() {
        val leapFebDay = LocalDate.of(2024, 2, 15)

        val period = MonthlyPeriod.of(leapFebDay)

        assertThat(period.yearMonthVal).isEqualTo("2024-02")
        assertThat(period.start).isEqualTo(LocalDate.of(2024, 2, 1))
        assertThat(period.end).isEqualTo(LocalDate.of(2024, 2, 29))
    }

    @DisplayName("평년 2월은 28일까지 반환된다 (2026년)")
    @Test
    fun nonLeapFebruaryEndsOn28th() {
        val nonLeapFebDay = LocalDate.of(2026, 2, 10)

        val period = MonthlyPeriod.of(nonLeapFebDay)

        assertThat(period.yearMonthVal).isEqualTo("2026-02")
        assertThat(period.start).isEqualTo(LocalDate.of(2026, 2, 1))
        assertThat(period.end).isEqualTo(LocalDate.of(2026, 2, 28))
    }

    @DisplayName("12월 입력은 해당 연도의 12-31 이 말일이 된다 (다음 해로 넘어가지 않는다)")
    @Test
    fun decemberDoesNotSpillIntoNextYear() {
        val decemberDay = LocalDate.of(2025, 12, 20)

        val period = MonthlyPeriod.of(decemberDay)

        assertThat(period.yearMonthVal).isEqualTo("2025-12")
        assertThat(period.start).isEqualTo(LocalDate.of(2025, 12, 1))
        assertThat(period.end).isEqualTo(LocalDate.of(2025, 12, 31))
    }

    @DisplayName("한 자리 월도 두 자리로 0-padding 된다")
    @Test
    fun singleDigitMonthIsZeroPadded() {
        val januaryDay = LocalDate.of(2026, 1, 15)

        val period = MonthlyPeriod.of(januaryDay)

        assertThat(period.yearMonthVal).isEqualTo("2026-01")
    }
}
