package com.loopers.domain.ranking

data class ProductRankResult(
    val productId: Long,
    val totalScore: Double,
    val viewCount: Int,
    val likeCount: Int,
    val orderCount: Int,
    val rank: Int,
)
