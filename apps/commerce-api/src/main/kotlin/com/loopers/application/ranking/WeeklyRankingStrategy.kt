package com.loopers.application.ranking

import com.loopers.application.product.ProductCacheManager
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
        return toRankingInfoList(entries, page, size, productCacheManager)
    }
}
