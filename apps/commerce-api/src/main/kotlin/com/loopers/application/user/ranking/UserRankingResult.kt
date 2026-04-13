package com.loopers.application.user.ranking

import java.math.BigDecimal

class UserRankingResult {
    data class RankedProduct(
        val rank: Long,
        val score: Double,
        val productId: Long,
        val productName: String,
        val sellingPrice: BigDecimal,
        val thumbnailUrl: String?,
    )
}
