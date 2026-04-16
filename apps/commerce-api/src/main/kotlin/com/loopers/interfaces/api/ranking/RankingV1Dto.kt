package com.loopers.interfaces.api.ranking

import com.loopers.application.ranking.RankingItemInfo

class RankingV1Dto {
    data class RankingItemResponse(
        val rank: Int,
        val score: Double,
        val productId: Long,
        val productName: String,
        val brandName: String?,
        val price: Long,
        val imageUrl: String,
        val likeCount: Int,
        val soldOut: Boolean,
    ) {
        companion object {
            fun from(info: RankingItemInfo): RankingItemResponse {
                return RankingItemResponse(
                    rank = info.rank,
                    score = info.score,
                    productId = info.product.id,
                    productName = info.product.name,
                    brandName = info.product.brandName,
                    price = info.product.price,
                    imageUrl = info.product.imageUrl,
                    likeCount = info.product.likeCount,
                    soldOut = info.product.soldOut,
                )
            }
        }
    }
}
