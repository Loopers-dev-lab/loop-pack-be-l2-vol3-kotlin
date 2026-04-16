package com.loopers.application.ranking

import com.loopers.application.product.ProductCacheManager
import com.loopers.domain.ranking.RankingEntry
import com.loopers.domain.ranking.WeeklyRankingRepository
import com.loopers.domain.ranking.YearWeek
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component(WeeklyRankingStrategy.BEAN_NAME)
class WeeklyRankingStrategy(
    private val weeklyRankingRepository: WeeklyRankingRepository,
    private val productCacheManager: ProductCacheManager,
) : RankingStrategy {

    companion object {
        const val BEAN_NAME = "weeklyRankingStrategy"
    }

    override fun getRankings(date: LocalDate, page: Int, size: Int): List<RankingInfo> {
        val yearWeek = YearWeek.from(date)
        val offset = ((page - 1) * size).toLong()
        val entries = weeklyRankingRepository.findTopRankings(yearWeek, offset, size.toLong())
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
