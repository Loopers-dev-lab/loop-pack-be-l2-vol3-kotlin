package com.loopers.domain.ranking

data class ProductRankingReadModel(
    val productId: Long,
    val rank: Long,
    val score: Double,
)
