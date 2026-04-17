package com.loopers.infrastructure.ranking

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

data class RankingPeriodDateRange(
    val startDate: LocalDate,
    val endDate: LocalDate,
)

object RankingPeriodDateRangeResolver {
    private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.BASIC_ISO_DATE

    fun parse(date: String): LocalDate = LocalDate.parse(date, DATE_FORMATTER)

    fun weekly(date: String): RankingPeriodDateRange = weekly(parse(date))

    fun weekly(date: LocalDate): RankingPeriodDateRange {
        val startDate = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        return RankingPeriodDateRange(
            startDate = startDate,
            endDate = startDate.plusDays(6),
        )
    }

    fun monthly(date: String): RankingPeriodDateRange = monthly(parse(date))

    fun monthly(date: LocalDate): RankingPeriodDateRange {
        val startDate = date.withDayOfMonth(1)
        return RankingPeriodDateRange(
            startDate = startDate,
            endDate = startDate.with(TemporalAdjusters.lastDayOfMonth()),
        )
    }
}
