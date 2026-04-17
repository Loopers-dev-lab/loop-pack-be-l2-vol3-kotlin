package com.loopers.interfaces.api.v1.ranking

import com.loopers.application.ranking.RankingInfo
import com.loopers.application.ranking.RankingPageInfo

data class GetRankingResponse(
    val rankings: List<RankingItemResponse>,
    val totalCount: Long,
    val page: Int,
    val size: Int,
) {
    companion object {
        fun from(pageInfo: RankingPageInfo) = GetRankingResponse(
            rankings = pageInfo.rankings.map { RankingItemResponse.from(it) },
            totalCount = pageInfo.totalCount,
            page = pageInfo.page,
            size = pageInfo.size,
        )
    }
}

data class RankingItemResponse(
    val rank: Long,
    val score: Double,
    val productId: Long,
    val productName: String,
    val price: Long,
    val thumbnailUrl: String?,
    val brandName: String,
    val likeCount: Int,
) {
    companion object {
        fun from(info: RankingInfo) = RankingItemResponse(
            rank = info.rank,
            score = info.score,
            productId = info.productId,
            productName = info.productName,
            price = info.price,
            thumbnailUrl = info.thumbnailUrl,
            brandName = info.brandName,
            likeCount = info.likeCount,
        )
    }
}
