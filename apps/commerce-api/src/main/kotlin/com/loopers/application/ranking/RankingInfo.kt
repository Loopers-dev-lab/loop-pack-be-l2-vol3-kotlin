package com.loopers.application.ranking

import com.loopers.application.product.ProductInfo

data class RankedEntry(
    val rank: Int,
    val score: Double,
    val productId: Long,
)

data class RankingPosition(
    val rank: Int,
    val score: Double,
)

data class RankingItemInfo(
    val rank: Int,
    val score: Double,
    val product: ProductInfo,
)

data class RankingPageInfo(
    val items: List<RankingItemInfo>,
    val totalElements: Long,
    val totalPages: Int,
)
