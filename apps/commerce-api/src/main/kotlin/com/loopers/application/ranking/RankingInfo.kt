package com.loopers.application.ranking

data class RankingInfo(
    val productId: Long,
    val rank: Long,
    val score: Double,
    val productName: String,
    val productPrice: Long,
)
