package com.loopers.domain.ranking

import java.time.YearMonth

interface MonthlyRankingRepository {
    fun findTopRankings(yearMonth: YearMonth, offset: Long, count: Long): List<RankingEntry>
}
