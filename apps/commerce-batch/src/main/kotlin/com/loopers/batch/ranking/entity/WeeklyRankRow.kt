package com.loopers.batch.ranking.entity

data class WeeklyRankRow(
    val productId: Long,
    val totalViewCount: Long,
    val totalLikeCount: Long,
    val totalSalesCount: Long,
    val score: Double,
)
