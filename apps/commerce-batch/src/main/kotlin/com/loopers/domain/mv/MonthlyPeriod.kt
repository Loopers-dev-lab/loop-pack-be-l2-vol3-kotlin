package com.loopers.domain.mv

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

/**
 * 월 경계(1일 ~ 말일) 를 표현하는 불변 값 객체.
 *
 * `yearMonthVal` 은 MV 조회 키로 쓰이며 `yyyy-MM` 포맷을 강제한다.
 * 윤년 2월(29일) 과 평년 2월(28일) 을 `YearMonth.atEndOfMonth` 가 자동 처리한다.
 */
data class MonthlyPeriod(
    val yearMonthVal: String,
    val start: LocalDate,
    val end: LocalDate,
) {
    companion object {
        private val YEAR_MONTH_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM")

        fun of(date: LocalDate): MonthlyPeriod {
            val yearMonth = YearMonth.from(date)
            return MonthlyPeriod(
                yearMonthVal = yearMonth.format(YEAR_MONTH_FORMATTER),
                start = yearMonth.atDay(1),
                end = yearMonth.atEndOfMonth(),
            )
        }
    }
}
