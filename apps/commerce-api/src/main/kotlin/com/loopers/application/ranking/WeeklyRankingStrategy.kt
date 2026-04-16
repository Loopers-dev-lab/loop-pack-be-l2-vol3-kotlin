package com.loopers.application.ranking

import com.loopers.application.product.ProductCacheManager
import com.loopers.domain.ranking.Period
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

    override fun getRankings(date: LocalDate, page: Int, size: Int): RankingResult {
        val yearWeek = YearWeek.from(date)
        val offset = pageToOffset(page, size)
        val entries = weeklyRankingRepository.findTopRankings(yearWeek, offset, size.toLong())
        return RankingResult(
            period = Period.WEEKLY,
            periodStart = yearWeek.startDate.toString(),
            periodEnd = yearWeek.endDate.toString(),
            items = toRankingInfoList(entries, page, size, productCacheManager),
        )
    }
}
