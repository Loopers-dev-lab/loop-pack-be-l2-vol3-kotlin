package com.loopers.interfaces.api.user.ranking

import com.loopers.application.user.ranking.UserRankingResult
import java.math.BigDecimal

class UserRankingV1Response {
    data class RankedProduct(
        val rank: Long,
        val score: Double,
        val productId: Long,
        val productName: String,
        val sellingPrice: BigDecimal,
        val thumbnailUrl: String?,
    ) {
        companion object {
            fun from(result: UserRankingResult.RankedProduct): RankedProduct =
                RankedProduct(
                    rank = result.rank,
                    score = result.score,
                    productId = result.productId,
                    productName = result.productName,
                    sellingPrice = result.sellingPrice,
                    thumbnailUrl = result.thumbnailUrl,
                )
        }
    }
}
