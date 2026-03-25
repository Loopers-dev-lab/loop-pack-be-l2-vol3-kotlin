package com.loopers.domain.productmetrics

interface ProductMetricsRepository {
    fun findByProductIdWithLock(productId: Long): ProductMetrics?
    fun save(metrics: ProductMetrics): ProductMetrics
}
