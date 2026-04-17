package com.loopers.infrastructure.ranking

import com.loopers.application.ranking.RankingPeriod
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component

@Component
class RankingMaterializedViewReader(
    private val weeklyProductRankingJpaRepository: WeeklyProductRankingJpaRepository,
    private val monthlyProductRankingJpaRepository: MonthlyProductRankingJpaRepository,
) {
    fun getPage(
        period: RankingPeriod,
        date: String,
        size: Int,
        page: Int,
    ): List<RankedProductScore> {
        val pageable = PageRequest.of(page - 1, size)
        return when (period) {
            RankingPeriod.WEEKLY -> {
                val weekStartDate = RankingPeriodDateRangeResolver.weekly(date).startDate
                weeklyProductRankingJpaRepository.findAllByWeekStartDateOrderByRankingAsc(weekStartDate, pageable)
                    .content
                    .map { ranked ->
                        RankedProductScore(
                            rank = ranked.ranking,
                            productId = ranked.productId,
                            score = ranked.score,
                        )
                    }
            }

            RankingPeriod.MONTHLY -> {
                val monthStartDate = RankingPeriodDateRangeResolver.monthly(date).startDate
                monthlyProductRankingJpaRepository.findAllByMonthStartDateOrderByRankingAsc(monthStartDate, pageable)
                    .content
                    .map { ranked ->
                        RankedProductScore(
                            rank = ranked.ranking,
                            productId = ranked.productId,
                            score = ranked.score,
                        )
                    }
            }

            RankingPeriod.DAILY -> error("daily ranking should be read from redis")
        }
    }
}
