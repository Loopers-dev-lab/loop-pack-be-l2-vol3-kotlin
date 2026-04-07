package com.loopers.application.ranking

import java.math.BigDecimal

data class RankingInfo(
    val rank: Int,
    val productId: Long,
    val productName: String,
    val brandName: String?,
    val price: BigDecimal,
    val imageUrl: String?,
    val score: Double,
)
