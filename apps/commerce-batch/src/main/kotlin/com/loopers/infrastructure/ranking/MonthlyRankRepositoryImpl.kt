package com.loopers.infrastructure.ranking

import com.loopers.domain.ranking.MonthlyRank
import com.loopers.domain.ranking.MonthlyRankRepository
import org.springframework.stereotype.Repository

@Repository
class MonthlyRankRepositoryImpl(
    private val jpa: MonthlyRankJpaRepository,
) : MonthlyRankRepository {

    override fun findLatestYearMonth(): String? = jpa.findLatestYearMonth()

    override fun findRanksByYearMonth(yearMonth: String): List<MonthlyRank> =
        jpa.findAllByIdYearMonthOrderByRankPositionAsc(yearMonth)

    override fun save(monthlyRank: MonthlyRank): MonthlyRank = jpa.save(monthlyRank)
}
