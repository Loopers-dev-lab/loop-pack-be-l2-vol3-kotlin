package com.loopers.domain.mv

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDate

@DisplayName("WeeklyPeriod — ISO 주 경계 계산")
class WeeklyPeriodTest {

    @DisplayName("월요일을 입력하면 같은 월요일이 시작일로 반환된다")
    @Test
    fun mondayInputReturnsSameMondayAsStart() {
        // 2026-04-13 (월요일)
        val monday = LocalDate.of(2026, 4, 13)

        val period = WeeklyPeriod.of(monday)

        assertThat(period.start).isEqualTo(LocalDate.of(2026, 4, 13))
        assertThat(period.end).isEqualTo(LocalDate.of(2026, 4, 19))
    }

    @DisplayName("주 중간의 수요일을 입력해도 그 주의 월요일과 일요일이 반환된다")
    @Test
    fun wednesdayInputNormalizesToMondayAndSunday() {
        // 2026-04-15 (수요일)
        val wednesday = LocalDate.of(2026, 4, 15)

        val period = WeeklyPeriod.of(wednesday)

        assertThat(period.start).isEqualTo(LocalDate.of(2026, 4, 13))
        assertThat(period.end).isEqualTo(LocalDate.of(2026, 4, 19))
    }

    @DisplayName("일요일을 입력하면 그 주의 월요일과 본인(일)이 반환된다")
    @Test
    fun sundayInputReturnsMondayAndSelf() {
        // 2026-04-19 (일요일)
        val sunday = LocalDate.of(2026, 4, 19)

        val period = WeeklyPeriod.of(sunday)

        assertThat(period.start).isEqualTo(LocalDate.of(2026, 4, 13))
        assertThat(period.end).isEqualTo(LocalDate.of(2026, 4, 19))
    }

    @DisplayName("월말/월초 경계를 걸쳐도 올바른 주 경계가 계산된다")
    @Test
    fun spansMonthBoundary() {
        // 2026-05-01 (금요일) — 해당 주는 2026-04-27(월) ~ 2026-05-03(일)
        val friday = LocalDate.of(2026, 5, 1)

        val period = WeeklyPeriod.of(friday)

        assertThat(period.start).isEqualTo(LocalDate.of(2026, 4, 27))
        assertThat(period.end).isEqualTo(LocalDate.of(2026, 5, 3))
    }

    @DisplayName("연말/연초 경계를 걸쳐도 올바른 주 경계가 계산된다")
    @Test
    fun spansYearBoundary() {
        // 2025-12-31 (수요일) — 해당 주는 2025-12-29(월) ~ 2026-01-04(일)
        val wednesday = LocalDate.of(2025, 12, 31)

        val period = WeeklyPeriod.of(wednesday)

        assertThat(period.start).isEqualTo(LocalDate.of(2025, 12, 29))
        assertThat(period.end).isEqualTo(LocalDate.of(2026, 1, 4))
    }
}
