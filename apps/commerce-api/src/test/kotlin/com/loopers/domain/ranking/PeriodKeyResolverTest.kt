package com.loopers.domain.ranking

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDate

@DisplayName("PeriodKeyResolver")
class PeriodKeyResolverTest {

    @Test
    @DisplayName("resolveWeekKey는 ISO 8601 주차를 yyyy-Www 포맷으로 반환한다")
    fun `주차 키 포맷`() {
        assertThat(PeriodKeyResolver.resolveWeekKey(LocalDate.of(2026, 4, 13))).isEqualTo("2026-W16")
        assertThat(PeriodKeyResolver.resolveWeekKey(LocalDate.of(2026, 4, 19))).isEqualTo("2026-W16")
        assertThat(PeriodKeyResolver.resolveWeekKey(LocalDate.of(2026, 1, 1))).isEqualTo("2026-W01")
    }

    @Test
    @DisplayName("resolveWeekKey는 연말/연시 경계에서 ISO 주차 규칙을 따른다")
    fun `연말 연시 경계`() {
        // 2025-12-29 (Mon) ~ 2026-01-04 (Sun) → ISO week 2026-W01
        assertThat(PeriodKeyResolver.resolveWeekKey(LocalDate.of(2025, 12, 29))).isEqualTo("2026-W01")
        assertThat(PeriodKeyResolver.resolveWeekKey(LocalDate.of(2026, 1, 4))).isEqualTo("2026-W01")
    }

    @Test
    @DisplayName("resolveMonthKey는 yyyyMM 포맷을 반환한다")
    fun `월간 키 포맷`() {
        assertThat(PeriodKeyResolver.resolveMonthKey(LocalDate.of(2026, 4, 13))).isEqualTo("202604")
        assertThat(PeriodKeyResolver.resolveMonthKey(LocalDate.of(2026, 12, 31))).isEqualTo("202612")
    }

    @Test
    @DisplayName("weekRange는 월요일~일요일 쌍을 반환한다")
    fun `주간 범위`() {
        val (start, end) = PeriodKeyResolver.weekRange(LocalDate.of(2026, 4, 13))
        assertThat(start).isEqualTo(LocalDate.of(2026, 4, 13))
        assertThat(end).isEqualTo(LocalDate.of(2026, 4, 19))

        val (start2, end2) = PeriodKeyResolver.weekRange(LocalDate.of(2026, 4, 19))
        assertThat(start2).isEqualTo(LocalDate.of(2026, 4, 13))
        assertThat(end2).isEqualTo(LocalDate.of(2026, 4, 19))
    }

    @Test
    @DisplayName("monthRange는 달력월 1일~말일을 반환한다")
    fun `월간 범위`() {
        val (start, end) = PeriodKeyResolver.monthRange(LocalDate.of(2026, 4, 13))
        assertThat(start).isEqualTo(LocalDate.of(2026, 4, 1))
        assertThat(end).isEqualTo(LocalDate.of(2026, 4, 30))

        val (feb1, feb28) = PeriodKeyResolver.monthRange(LocalDate.of(2025, 2, 15))
        assertThat(feb1).isEqualTo(LocalDate.of(2025, 2, 1))
        assertThat(feb28).isEqualTo(LocalDate.of(2025, 2, 28))
    }
}
