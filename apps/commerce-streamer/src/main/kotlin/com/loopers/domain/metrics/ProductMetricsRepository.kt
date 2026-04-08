package com.loopers.domain.metrics

interface ProductMetricsRepository {
    fun findByProductId(productId: Long): ProductMetrics?
    fun findAll(): List<ProductMetrics>
    fun save(productMetrics: ProductMetrics): ProductMetrics
}
