package com.loopers.common

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.ZoneId

class DateUtilsTest {

    @DisplayName("KST 날짜 계산")
    @Nested
    inner class KstDates {

        @DisplayName("todayKst는 서울 기준 오늘을 반환한다")
        @Test
        fun today() {
            val result = DateUtils.todayKst()
            val expected = LocalDate.now(ZoneId.of("Asia/Seoul"))
            assertThat(result).isEqualTo(expected)
        }

        @DisplayName("yesterdayKst는 todayKst보다 하루 전이다")
        @Test
        fun yesterday() {
            assertThat(DateUtils.yesterdayKst()).isEqualTo(DateUtils.todayKst().minusDays(1))
        }

        @DisplayName("tomorrowKst는 todayKst보다 하루 후이다")
        @Test
        fun tomorrow() {
            assertThat(DateUtils.tomorrowKst()).isEqualTo(DateUtils.todayKst().plusDays(1))
        }
    }

    @DisplayName("yyyyMMdd 포맷 변환")
    @Nested
    inner class Format {

        @DisplayName("formatDate는 yyyyMMdd 문자열로 반환한다")
        @Test
        fun format() {
            val result = DateUtils.formatDate(LocalDate.of(2026, 4, 17))
            assertThat(result).isEqualTo("20260417")
        }

        @DisplayName("parseDate는 yyyyMMdd 문자열을 LocalDate로 복원한다")
        @Test
        fun parse() {
            val result = DateUtils.parseDate("20260417")
            assertThat(result).isEqualTo(LocalDate.of(2026, 4, 17))
        }
    }
}
