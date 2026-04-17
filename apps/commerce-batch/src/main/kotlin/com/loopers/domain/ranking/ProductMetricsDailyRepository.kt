package com.loopers.domain.ranking

import java.time.LocalDate

interface ProductMetricsDailyRepository {

    fun findDailyOrNull(productId: Long, date: LocalDate): ProductMetricsDaily?

    fun findAllDailyOn(date: LocalDate): List<ProductMetricsDaily>

    fun countDailyOn(date: LocalDate): Long

    fun save(daily: ProductMetricsDaily): ProductMetricsDaily
}
