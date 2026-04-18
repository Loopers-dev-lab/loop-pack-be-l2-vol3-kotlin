package com.loopers.batch.job.ranking

data class ProductRankRow(
    val periodKey: String,
    val productId: Long,
    val score: Double,
    val viewCount: Long,
    val likeCount: Long,
    val salesCount: Long,
)
