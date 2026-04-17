package com.loopers.batch.job.ranking

data class AggregatedProductRankingRow(
    val productId: Long,
    val likeCount: Long,
    val viewCount: Long,
    val salesCount: Long,
    val score: Double,
)
