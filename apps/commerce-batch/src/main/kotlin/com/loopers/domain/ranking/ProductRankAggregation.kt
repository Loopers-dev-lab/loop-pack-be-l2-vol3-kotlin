package com.loopers.domain.ranking

data class ProductRankAggregation(
    val productId: Long,
    val totalScore: Double,
    val viewCount: Int,
    val likeCount: Int,
    val orderCount: Int,
)
