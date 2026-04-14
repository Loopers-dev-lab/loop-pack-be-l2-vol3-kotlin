package com.loopers.application.ranking

data class RankingPageResult(
    val date: String, // yyyyMMdd
    val page: Int,
    val size: Int,
    val totalCount: Long,
    val items: List<RankingItemResult>,
)

data class RankingItemResult(
    val rank: Long, // 0-based
    val score: Double,
    val productId: Long,
    val name: String,
    val price: Int,
    val likeCount: Int,
    val brandId: Long,
    val brandName: String,
)
