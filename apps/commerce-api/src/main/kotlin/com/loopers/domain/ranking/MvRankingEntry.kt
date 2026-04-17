package com.loopers.domain.ranking

data class MvRankingEntry(
    val productId: Long,
    val rank: Int,
    val score: Double,
)
