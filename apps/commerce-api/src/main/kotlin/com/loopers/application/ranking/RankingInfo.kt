package com.loopers.application.ranking

data class RankingInfo(
    val rank: Long,
    val score: Double,
    val productId: Long,
    val productName: String,
    val price: Long,
    val thumbnailUrl: String?,
    val brandName: String,
    val likeCount: Int,
)

data class RankingPageInfo(
    val rankings: List<RankingInfo>,
    val totalCount: Long,
    val page: Int,
    val size: Int,
)

data class ProductRankInfo(
    val rank: Long,
    val score: Double,
)
