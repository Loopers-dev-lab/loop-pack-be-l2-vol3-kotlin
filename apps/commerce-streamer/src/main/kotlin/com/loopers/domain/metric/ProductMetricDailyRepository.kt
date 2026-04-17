package com.loopers.domain.metric

import java.time.LocalDate

interface ProductMetricDailyRepository {
    fun findByProductIdAndMetricDate(productId: Long, metricDate: LocalDate): ProductMetricDaily?

    fun save(metric: ProductMetricDaily): ProductMetricDaily
}
