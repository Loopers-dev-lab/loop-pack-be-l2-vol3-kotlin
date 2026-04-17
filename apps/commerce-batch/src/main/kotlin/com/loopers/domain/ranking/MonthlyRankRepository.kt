package com.loopers.domain.ranking

interface MonthlyRankRepository {

    fun findLatestYearMonth(): String?

    fun findRanksByYearMonth(yearMonth: String): List<MonthlyRank>

    fun save(monthlyRank: MonthlyRank): MonthlyRank
}
