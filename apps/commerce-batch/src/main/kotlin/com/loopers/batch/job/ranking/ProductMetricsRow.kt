package com.loopers.batch.job.ranking

data class ProductMetricsRow(
    val productId: Long,
    val score: Double,
)
