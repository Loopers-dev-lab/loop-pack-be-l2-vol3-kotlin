package com.loopers.application.ranking

data class RankingProductInfo(
    val rank: Long,
    val score: Double,
    val productId: Long,
    val productName: String,
    val productPrice: Long,
    val brandId: Long,
    val brandName: String,
)
