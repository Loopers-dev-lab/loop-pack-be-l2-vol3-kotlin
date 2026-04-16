package com.loopers.application.ranking

import com.loopers.application.product.ProductCacheManager
import com.loopers.domain.ranking.MonthlyRankingRepository
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
        return toRankingInfoList(entries, page, size, productCacheManager)
    }
}
