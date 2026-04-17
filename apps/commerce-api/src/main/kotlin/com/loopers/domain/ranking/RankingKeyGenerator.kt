package com.loopers.domain.ranking

import java.time.LocalDate
import java.time.format.DateTimeFormatter

object RankingKeyGenerator {
    private const val KEY_PREFIX = "ranking:all"
    private val DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE

    fun dailyKey(date: LocalDate): String {
        return "$KEY_PREFIX:${date.format(DATE_FORMATTER)}"
    }
}
