package com.loopers.domain.ranking

interface WeeklyRankingRepository {
    fun findTopRankings(yearWeek: YearWeek, offset: Long, count: Long): List<RankingEntry>
}
