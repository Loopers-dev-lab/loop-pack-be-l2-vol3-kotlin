package com.loopers.domain.ranking

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.IsoFields
import java.time.temporal.TemporalAdjusters

@JvmInline
value class YearWeek private constructor(private val value: String) {

    val startDate: LocalDate
        get() {
            val (year, week) = parse()
            return LocalDate.of(year, 1, 4) // 1월 4일은 항상 W01에 속함
                .with(IsoFields.WEEK_OF_WEEK_BASED_YEAR, week.toLong())
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        }

    val endDate: LocalDate
        get() = startDate.plusDays(6)

    private fun parse(): Pair<Int, Int> {
        val parts = value.split("-W")
        return parts[0].toInt() to parts[1].toInt()
    }

    override fun toString(): String = value

    companion object {
        fun from(date: LocalDate): YearWeek {
            val weekYear = date.get(IsoFields.WEEK_BASED_YEAR)
            val weekNum = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
            return YearWeek("$weekYear-W${weekNum.toString().padStart(2, '0')}")
        }
    }
}
