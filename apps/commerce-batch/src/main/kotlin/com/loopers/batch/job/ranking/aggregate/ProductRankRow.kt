package com.loopers.batch.job.ranking.aggregate

data class ProductRankRow(
    val productId: Long,
    val score: Double,
    val viewCount: Long,
    val likeCount: Long,
    val orderCount: Long,
    val orderAmountSum: Long,
)
