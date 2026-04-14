package com.loopers.interfaces.api.ranking

import com.loopers.application.ranking.RankingProductInfo

class RankingV1Dto {
    data class RankingResponse(
        val rank: Long,
        val score: Double,
        val productId: Long,
        val productName: String,
        val productPrice: Long,
        val brandId: Long,
        val brandName: String,
    ) {
        companion object {
            fun from(info: RankingProductInfo): RankingResponse {
                return RankingResponse(
                    rank = info.rank,
                    score = info.score,
                    productId = info.productId,
                    productName = info.productName,
                    productPrice = info.productPrice,
                    brandId = info.brandId,
                    brandName = info.brandName,
                )
            }
        }
    }
}
