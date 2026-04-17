package com.loopers.common

import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object DateUtils {

    val KST: ZoneId = ZoneId.of("Asia/Seoul")

    private val YYYYMMDD: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
    private val YYYYMM: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMM")

    fun todayKst(): LocalDate = LocalDate.now(KST)

    fun yesterdayKst(): LocalDate = todayKst().minusDays(1)

    fun tomorrowKst(): LocalDate = todayKst().plusDays(1)

    fun formatDate(date: LocalDate): String = date.format(YYYYMMDD)

    fun parseDate(text: String): LocalDate = LocalDate.parse(text, YYYYMMDD)

    fun formatYearMonth(date: LocalDate): String = date.format(YYYYMM)
}
