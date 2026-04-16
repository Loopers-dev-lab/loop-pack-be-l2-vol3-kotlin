package com.loopers.interfaces.api.ranking

import com.loopers.application.ranking.RankingInfo

class RankingV1Dto {
    data class RankedProductResponse(
        val rank: Long,
        val productId: Long,
        val score: Double,
        val brandId: Long,
        val brandName: String,
        val name: String,
        val price: Long,
        val stock: Int,
        val status: String,
        val likeCount: Long,
    ) {
        companion object {
            fun from(info: RankingInfo.RankedProduct) = RankedProductResponse(
                rank = info.rank,
                productId = info.productId,
                score = info.score,
                brandId = info.brandId,
                brandName = info.brandName,
                name = info.name,
                price = info.price,
                stock = info.stock,
                status = info.status,
                likeCount = info.likeCount,
            )
        }
    }
}
