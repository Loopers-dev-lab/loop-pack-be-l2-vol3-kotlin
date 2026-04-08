package com.loopers.application.ranking

import com.loopers.application.product.ProductCacheManager
import com.loopers.domain.ranking.RankingService
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class RankingFacade(
    private val rankingService: RankingService,
    private val productCacheManager: ProductCacheManager,
) {

    fun getRankings(date: LocalDate, page: Int, size: Int): List<RankingInfo> {
        val entries = rankingService.getTopRankings(date, page, size)
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
}
