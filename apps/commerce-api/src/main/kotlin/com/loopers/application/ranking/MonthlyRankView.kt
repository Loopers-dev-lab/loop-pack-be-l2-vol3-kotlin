package com.loopers.application.ranking

data class MonthlyRankView(
    val productId: Long,
    val yearMonth: String,
    val viewCount: Long,
    val likeCount: Long,
    val orderCount: Long,
    val totalScore: Double,
    val rankPosition: Int,
)
