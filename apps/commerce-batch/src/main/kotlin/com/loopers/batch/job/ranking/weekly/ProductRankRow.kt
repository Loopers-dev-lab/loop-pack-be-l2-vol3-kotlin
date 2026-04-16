package com.loopers.batch.job.ranking.weekly

data class ProductRankRow(
    val yearWeek: String,
    val productId: Long,
    val score: Double,
    val viewCount: Long,
    val likeCount: Long,
    val salesCount: Long,
)
