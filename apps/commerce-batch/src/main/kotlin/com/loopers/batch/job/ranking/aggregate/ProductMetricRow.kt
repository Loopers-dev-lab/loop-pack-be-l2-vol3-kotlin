package com.loopers.batch.job.ranking.aggregate

data class ProductMetricRow(
    val productId: Long,
    val viewCount: Long,
    val likeCount: Long,
    val orderCount: Long,
    val orderAmountSum: Long,
)
