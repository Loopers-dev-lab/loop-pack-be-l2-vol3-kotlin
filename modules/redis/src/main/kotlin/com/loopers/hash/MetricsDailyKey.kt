package com.loopers.hash

import com.loopers.common.DateUtils
import java.time.LocalDate

object MetricsDailyKey {

    private const val KEY_PREFIX = "metrics:daily:"

    fun key(date: LocalDate): String = "$KEY_PREFIX${DateUtils.formatDate(date)}"

    fun field(productId: Long, type: MetricType): String = "$productId:${type.code}"

    fun parseField(field: String): Pair<Long, MetricType>? {
        val parts = field.split(":")
        if (parts.size != 2) return null
        val productId = parts[0].toLongOrNull() ?: return null
        val type = MetricType.fromCode(parts[1]) ?: return null
        return productId to type
    }
}
