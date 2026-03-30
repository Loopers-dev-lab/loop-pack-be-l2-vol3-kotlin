package com.loopers.domain.metrics

interface ProductMetricsRepository {
    fun findByProductId(productId: Long): ProductMetrics?
    fun incrementLikeCount(productId: Long, version: Long)
    fun decrementLikeCount(productId: Long, version: Long)
    fun incrementSalesCount(productId: Long, quantity: Int)
    fun incrementViewCount(productId: Long, version: Long)
    fun getVersion(productId: Long): Long?
}
