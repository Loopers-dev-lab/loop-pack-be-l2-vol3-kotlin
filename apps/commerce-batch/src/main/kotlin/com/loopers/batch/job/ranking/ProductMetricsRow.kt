package com.loopers.batch.job.ranking

data class ProductMetricsRow(
    val productId: Long,
    val viewCount: Long,
    val likeCount: Long,
    val orderCount: Long,
    val salesAmount: Long,
    val score: Double,
)
