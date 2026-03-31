package com.loopers.domain.metric

interface ProductMetricRepository {
    fun findByProductId(productId: Long): ProductMetric?
    fun save(metric: ProductMetric): ProductMetric
    fun saveAll(metrics: List<ProductMetric>): List<ProductMetric>
}
