package com.loopers.batch.job.ranking.aggregate

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.IsoFields
import java.time.temporal.TemporalAdjusters

object PeriodKeyResolver {

    private val MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM")

    fun resolveWeekKey(date: LocalDate): String {
        val weekBasedYear = date.get(IsoFields.WEEK_BASED_YEAR)
        val weekOfYear = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
        return "%04d-W%02d".format(weekBasedYear, weekOfYear)
    }

    fun resolveMonthKey(date: LocalDate): String = date.format(MONTH_FORMATTER)

    fun weekRange(date: LocalDate): Pair<LocalDate, LocalDate> {
        val monday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        return monday to monday.plusDays(6)
    }

    fun monthRange(date: LocalDate): Pair<LocalDate, LocalDate> {
        val first = date.withDayOfMonth(1)
        return first to date.with(TemporalAdjusters.lastDayOfMonth())
    }
}
