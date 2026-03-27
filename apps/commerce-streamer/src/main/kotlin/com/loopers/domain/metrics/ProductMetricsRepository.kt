package com.loopers.domain.metrics

interface ProductMetricsRepository {
    fun findByProductId(productId: Long): ProductMetrics?
    fun save(productMetrics: ProductMetrics): ProductMetrics
    fun incrementLikeCount(productId: Long, eventVersion: Long): Int
    fun decrementLikeCount(productId: Long, eventVersion: Long): Int
    fun incrementViewCount(productId: Long, eventVersion: Long): Int
    fun incrementOrderCount(productId: Long, eventVersion: Long): Int
}
