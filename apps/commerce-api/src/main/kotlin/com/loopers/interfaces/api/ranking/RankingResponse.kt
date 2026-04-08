package com.loopers.interfaces.api.ranking

import com.loopers.application.ranking.RankingProductInfo

data class RankingResponse(
    val rank: Int,
    val score: Double,
    val productId: Long,
    val productName: String,
    val price: Long,
    val brandName: String,
    val imageUrl: String,
    val likeCount: Long,
    val available: Boolean,
) {
    companion object {
        fun from(info: RankingProductInfo): RankingResponse = RankingResponse(
            rank = info.rank,
            score = info.score,
            productId = info.productId,
            productName = info.productName,
            price = info.price,
            brandName = info.brandName,
            imageUrl = info.imageUrl,
            likeCount = info.likeCount,
            available = info.available,
        )
    }
}
