package com.loopers.application.ranking

import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.WeekFields

object PeriodKeyCalculator {

    fun weekly(date: LocalDate): String {
        val weekFields = WeekFields.ISO
        val yearWeek = date.get(weekFields.weekBasedYear())
        val weekNum = date.get(weekFields.weekOfWeekBasedYear())
        return "%d-W%02d".format(yearWeek, weekNum)
    }

    fun monthly(date: LocalDate): String {
        return YearMonth.from(date).toString()
    }
}
