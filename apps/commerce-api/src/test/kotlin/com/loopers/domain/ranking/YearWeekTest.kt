package com.loopers.domain.ranking

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

@DisplayName("YearWeek")
class YearWeekTest {

    @DisplayName("from(LocalDate) 변환 시,")
    @Nested
    inner class From {

        @DisplayName("일반적인 날짜에서 올바른 ISO Week를 반환한다.")
        @Test
        fun convertsNormalDate() {
            // arrange
            val date = LocalDate.of(2026, 4, 15) // 수요일

            // act
            val yearWeek = YearWeek.from(date)

            // assert
            assertThat(yearWeek.toString()).isEqualTo("2026-W16")
        }

        @DisplayName("주의 시작일(월요일)과 종료일(일요일)을 정확히 계산한다.")
        @Test
        fun calculatesStartAndEndDate() {
            // arrange
            val date = LocalDate.of(2026, 4, 15) // 수요일, W16

            // act
            val yearWeek = YearWeek.from(date)

            // assert
            assertThat(yearWeek.startDate).isEqualTo(LocalDate.of(2026, 4, 13)) // 월요일
            assertThat(yearWeek.endDate).isEqualTo(LocalDate.of(2026, 4, 19)) // 일요일
        }
    }

    @DisplayName("ISO Week 엣지 케이스:")
    @Nested
    inner class EdgeCases {

        @DisplayName("2022-01-01(토)은 2021-W52에 속한다.")
        @Test
        fun jan1BelongsToPreviousYear() {
            // arrange
            val date = LocalDate.of(2022, 1, 1) // 토요일

            // act
            val yearWeek = YearWeek.from(date)

            // assert
            assertThat(yearWeek.toString()).isEqualTo("2021-W52")
        }

        @DisplayName("2025-12-29(월)은 2026-W01에 속한다.")
        @Test
        fun dec29BelongsToNextYear() {
            // arrange
            val date = LocalDate.of(2025, 12, 29) // 월요일

            // act
            val yearWeek = YearWeek.from(date)

            // assert
            assertThat(yearWeek.toString()).isEqualTo("2026-W01")
        }

        @DisplayName("주 번호가 한 자리여도 두 자리로 패딩된다 (W01).")
        @Test
        fun weekNumberIsPadded() {
            // arrange
            val date = LocalDate.of(2026, 1, 5) // 월요일, W02

            // act
            val yearWeek = YearWeek.from(date)

            // assert
            assertThat(yearWeek.toString()).isEqualTo("2026-W02")
        }
    }
}
