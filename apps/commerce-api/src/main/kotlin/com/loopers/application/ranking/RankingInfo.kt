package com.loopers.application.ranking

import java.time.LocalDate

data class RankingProductInfo(
    val rank: Int,
    val score: Double,
    val productId: Long,
    val productName: String,
    val price: Long,
    val brandName: String,
    val imageUrl: String,
    val likeCount: Long,
    val available: Boolean,
    val weekStart: LocalDate? = null,
    val weekEnd: LocalDate? = null,
    val yearMonth: String? = null,
)
