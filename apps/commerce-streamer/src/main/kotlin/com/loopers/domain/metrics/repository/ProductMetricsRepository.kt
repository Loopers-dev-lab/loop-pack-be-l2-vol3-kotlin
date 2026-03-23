package com.loopers.domain.metrics.repository

import com.loopers.domain.metrics.model.ProductMetrics

interface ProductMetricsRepository {
    fun findByProductId(productId: Long): ProductMetrics?
    fun save(metrics: ProductMetrics): ProductMetrics
}
