package com.loopers.interfaces.api.ranking

import com.loopers.application.ranking.RankingProductInfo
import java.time.LocalDate

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
    val weekStart: LocalDate? = null,
    val weekEnd: LocalDate? = null,
    val yearMonth: String? = null,
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
            weekStart = info.weekStart,
            weekEnd = info.weekEnd,
            yearMonth = info.yearMonth,
        )
    }
}
