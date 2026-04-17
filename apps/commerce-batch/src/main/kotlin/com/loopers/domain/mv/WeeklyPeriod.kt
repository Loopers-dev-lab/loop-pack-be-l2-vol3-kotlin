package com.loopers.domain.mv

import java.time.LocalDate
import java.time.temporal.WeekFields

/**
 * ISO 주 경계(월요일 ~ 일요일) 를 표현하는 불변 값 객체.
 *
 * `WeekFields.ISO` 를 사용하여 한국식 주 시작 요일(월)과 일치시킨다.
 * JVM 기본 로케일에 의존하지 않도록 명시적으로 ISO 를 지정한다.
 */
data class WeeklyPeriod(
    val start: LocalDate,
    val end: LocalDate,
) {
    companion object {
        fun of(date: LocalDate): WeeklyPeriod {
            val monday = date.with(WeekFields.ISO.dayOfWeek(), 1)
            return WeeklyPeriod(start = monday, end = monday.plusDays(6))
        }
    }
}
