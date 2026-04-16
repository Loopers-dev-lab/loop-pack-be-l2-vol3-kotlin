package com.loopers.zset

import com.loopers.common.DateUtils
import java.time.LocalDate

object RankingKeyGenerator {

    private const val KEY_PREFIX = "ranking:daily:"

    fun dailyKey(date: LocalDate): String = "$KEY_PREFIX${DateUtils.formatDate(date)}"

    fun todayKey(): String = dailyKey(DateUtils.todayKst())
}
