package com.loopers.domain.ranking

import java.time.DayOfWeek
import java.time.LocalDate

enum class RankingPeriodType {
    WEEKLY,
    MONTHLY,
    ;

    fun periodStartDate(date: LocalDate): LocalDate =
        when (this) {
            WEEKLY -> date.with(DayOfWeek.MONDAY)
            MONTHLY -> date.withDayOfMonth(1)
        }

    fun periodEndDate(date: LocalDate): LocalDate =
        when (this) {
            WEEKLY -> date.with(DayOfWeek.SUNDAY)
            MONTHLY -> date.withDayOfMonth(date.lengthOfMonth())
        }
}
