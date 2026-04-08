package com.loopers.domain.ranking

import java.time.LocalDate

interface RankingRepository {
    fun getTopRankings(date: LocalDate, offset: Long, count: Long): List<RankingEntry>
    fun getRank(date: LocalDate, productId: Long): Long?
    fun carryOver(fromDate: LocalDate, toDate: LocalDate, weight: Double)
}
