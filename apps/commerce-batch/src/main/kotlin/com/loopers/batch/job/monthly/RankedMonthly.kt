package com.loopers.batch.job.monthly

data class RankedMonthly(
    val productId: Long,
    val yearMonth: String,
    val viewCount: Long,
    val likeCount: Long,
    val orderCount: Long,
    val totalScore: Double,
    val rankPosition: Int,
)
