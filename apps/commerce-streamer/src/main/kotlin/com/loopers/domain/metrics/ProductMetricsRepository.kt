package com.loopers.domain.metrics

interface ProductMetricsRepository {
    fun findByProductId(productId: Long): ProductMetrics?
    fun findByProductIds(productIds: Set<Long>): List<ProductMetrics>
    fun save(metrics: ProductMetrics): ProductMetrics
}
