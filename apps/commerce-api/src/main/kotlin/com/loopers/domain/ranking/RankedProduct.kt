package com.loopers.domain.ranking

data class RankedProduct(
    val productId: Long,
    val score: Double,
    val rank: Long,
)
