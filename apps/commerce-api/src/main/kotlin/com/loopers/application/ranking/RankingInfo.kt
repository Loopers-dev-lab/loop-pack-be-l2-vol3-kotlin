package com.loopers.application.ranking

class RankingInfo {
    data class RankedProduct(
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
    )
}
