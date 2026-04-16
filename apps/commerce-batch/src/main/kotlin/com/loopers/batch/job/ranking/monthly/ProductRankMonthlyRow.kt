package com.loopers.batch.job.ranking.monthly

data class ProductRankMonthlyRow(
    val yearMonth: String,
    val productId: Long,
    val score: Double,
    val viewCount: Long,
    val likeCount: Long,
    val salesCount: Long,
)
