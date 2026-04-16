package com.loopers.domain.metrics

import java.time.LocalDate

interface ProductMetricsRepository {
    fun findByProductIdAndMetricDate(productId: Long, metricDate: LocalDate): ProductMetrics?
    fun findByProductIdsAndMetricDate(productIds: Set<Long>, metricDate: LocalDate): List<ProductMetrics>
    fun save(metrics: ProductMetrics): ProductMetrics
}
