package com.loopers.infrastructure.ranking

import com.loopers.domain.ranking.MvRankingEntry
import com.loopers.domain.ranking.MvRankingRepository
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component

@Component
@Qualifier("weekly")
class WeeklyMvRankingRepositoryImpl(
    private val jpa: MvProductRankWeeklyJpaRepository,
) : MvRankingRepository {

    override fun findTop(periodKey: String, page: Int, size: Int): List<MvRankingEntry> {
        val pageable = PageRequest.of(page.coerceAtLeast(0), size.coerceAtLeast(1))
        return jpa.findByPeriodKeyOrderByRankValueAsc(periodKey, pageable)
            .map { MvRankingEntry(productId = it.productId, rank = it.rankValue, score = it.score) }
    }

    override fun count(periodKey: String): Long = jpa.countByPeriodKey(periodKey)
}

@Component
@Qualifier("monthly")
class MonthlyMvRankingRepositoryImpl(
    private val jpa: MvProductRankMonthlyJpaRepository,
) : MvRankingRepository {

    override fun findTop(periodKey: String, page: Int, size: Int): List<MvRankingEntry> {
        val pageable = PageRequest.of(page.coerceAtLeast(0), size.coerceAtLeast(1))
        return jpa.findByPeriodKeyOrderByRankValueAsc(periodKey, pageable)
            .map { MvRankingEntry(productId = it.productId, rank = it.rankValue, score = it.score) }
    }

    override fun count(periodKey: String): Long = jpa.countByPeriodKey(periodKey)
}
