package com.loopers.domain.metrics.repository

import com.loopers.domain.metrics.model.ProductMetricsDaily
import java.time.LocalDate

interface ProductMetricsDailyRepository {
    fun findByDateAndProductId(metricDate: LocalDate, productId: Long): ProductMetricsDaily?
    fun save(daily: ProductMetricsDaily): ProductMetricsDaily
}
