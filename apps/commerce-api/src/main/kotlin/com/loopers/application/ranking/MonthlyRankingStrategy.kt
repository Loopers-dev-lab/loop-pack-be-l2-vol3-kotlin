package com.loopers.application.ranking

import com.loopers.application.product.ProductCacheManager
import com.loopers.domain.ranking.MonthlyRankingRepository
import com.loopers.domain.ranking.RankingEntry
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.YearMonth

@Component(MonthlyRankingStrategy.BEAN_NAME)
class MonthlyRankingStrategy(
    private val monthlyRankingRepository: MonthlyRankingRepository,
    private val productCacheManager: ProductCacheManager,
) : RankingStrategy {

    companion object {
        const val BEAN_NAME = "monthlyRankingStrategy"
    }

    override fun getRankings(date: LocalDate, page: Int, size: Int): List<RankingInfo> {
        val yearMonth = YearMonth.from(date)
        val offset = ((page - 1) * size).toLong()
        val entries = monthlyRankingRepository.findTopRankings(yearMonth, offset, size.toLong())
        return toRankingInfoList(entries, page, size)
    }

    private fun toRankingInfoList(entries: List<RankingEntry>, page: Int, size: Int): List<RankingInfo> {
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
