package com.loopers.domain.ranking

import java.time.DayOfWeek
import java.time.LocalDate

enum class RankingPeriodType {
    DAILY,
    WEEKLY,
    MONTHLY,
    ;

    fun periodStartDate(date: LocalDate): LocalDate =
        when (this) {
            DAILY -> date
            WEEKLY -> date.with(DayOfWeek.MONDAY)
            MONTHLY -> date.withDayOfMonth(1)
        }
}
