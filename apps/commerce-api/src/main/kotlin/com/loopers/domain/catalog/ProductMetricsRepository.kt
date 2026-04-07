package com.loopers.domain.catalog

interface ProductMetricsRepository {
    fun findByProductId(productId: Long): ProductMetricsModel?
    fun save(metrics: ProductMetricsModel): ProductMetricsModel
    fun saveAll(metrics: List<ProductMetricsModel>): List<ProductMetricsModel>
}
