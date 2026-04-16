package com.loopers.application.ranking

import com.loopers.domain.ranking.Period
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class RankingFacade(
    private val strategies: Map<String, RankingStrategy>,
) {

    fun getRankings(date: LocalDate, period: Period, page: Int, size: Int): List<RankingInfo> {
        val strategy = resolveStrategy(period)
        return strategy.getRankings(date, page, size)
    }

    private fun resolveStrategy(period: Period): RankingStrategy {
        val beanName = when (period) {
            Period.DAILY -> DailyRankingStrategy.BEAN_NAME
            Period.WEEKLY -> WeeklyRankingStrategy.BEAN_NAME
            Period.MONTHLY -> MonthlyRankingStrategy.BEAN_NAME
        }
        return strategies[beanName]
            ?: throw CoreException(ErrorType.INTERNAL_ERROR, "지원하지 않는 기간: $period")
    }
}
