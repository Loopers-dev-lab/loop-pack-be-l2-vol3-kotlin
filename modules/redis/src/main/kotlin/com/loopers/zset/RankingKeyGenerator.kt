package com.loopers.zset

import java.time.LocalDate
import java.time.format.DateTimeFormatter

object RankingKeyGenerator {

    private const val KEY_PREFIX = "ranking:daily:"
    private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd")

    fun dailyKey(date: LocalDate): String {
        return "$KEY_PREFIX${date.format(DATE_FORMATTER)}"
    }

    fun todayKey(): String = dailyKey(LocalDate.now())
}
