package com.loopers.domain.ranking

import java.time.LocalDate

interface MonthlyRankingRepository {

    fun findRankings(rankingDate: LocalDate, offset: Long, size: Long): List<RankingEntry>

    fun countByRankingDate(rankingDate: LocalDate): Long
}
