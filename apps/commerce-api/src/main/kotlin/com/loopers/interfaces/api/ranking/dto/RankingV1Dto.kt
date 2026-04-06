package com.loopers.interfaces.api.ranking.dto

import com.loopers.application.ranking.RankingInfo
import java.math.BigDecimal

class RankingV1Dto {

    data class RankingResponse(
        val rank: Int,
        val productId: Long,
        val productName: String,
        val price: BigDecimal,
        val score: Double,
    ) {
        companion object {
            fun from(info: RankingInfo): RankingResponse = RankingResponse(
                rank = info.rank,
                productId = info.productId,
                productName = info.productName,
                price = info.price,
                score = info.score,
            )
        }
    }
}
