package com.loopers.domain.metrics

import java.time.LocalDate

interface ProductMetricsRepository {
    fun findByProductId(productId: Long): ProductMetrics?
    fun incrementLikeCount(productId: Long, version: Long)
    fun decrementLikeCount(productId: Long, version: Long)
    fun incrementSalesCount(productId: Long, quantity: Int)
    fun incrementViewCount(productId: Long, version: Long)
    fun getVersion(productId: Long): Long?
    fun incrementDailyViewCount(productId: Long, metricDate: LocalDate)
    fun incrementDailyLikeCount(productId: Long, metricDate: LocalDate)
    fun decrementDailyLikeCount(productId: Long, metricDate: LocalDate)
    fun incrementDailySalesCount(productId: Long, metricDate: LocalDate, quantity: Int)
}
