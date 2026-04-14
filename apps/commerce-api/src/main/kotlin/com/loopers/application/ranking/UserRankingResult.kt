package com.loopers.application.ranking

import java.math.BigDecimal
import java.time.LocalDate

data class RankedProductResult(
    val rank: Long,
    val score: Double,
    val productId: Long,
    val name: String,
    val price: BigDecimal,
    val brandId: Long,
    val brandName: String,
)

data class GetRankingResult(
    val date: LocalDate,
    val page: Int,
    val size: Int,
    val totalCount: Long,
    val hasNext: Boolean,
    val items: List<RankedProductResult>,
)
