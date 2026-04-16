package com.loopers.application.ranking

import com.loopers.application.product.ProductCacheManager
import com.loopers.domain.ranking.RankingEntry
import java.time.LocalDate

interface RankingStrategy {
    fun getRankings(date: LocalDate, page: Int, size: Int): List<RankingInfo>
}

fun toRankingInfoList(
    entries: List<RankingEntry>,
    page: Int,
    size: Int,
    productCacheManager: ProductCacheManager,
): List<RankingInfo> {
    val startRank = ((page - 1) * size).toLong()
    return entries.mapIndexed { index, entry ->
        val product = productCacheManager.getProduct(entry.productId)
        RankingInfo(
            productId = entry.productId,
            rank = startRank + index + 1,
            score = entry.score,
            productName = product.name,
            productPrice = product.price,
        )
    }
}
