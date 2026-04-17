package com.loopers.domain.ranking

import com.loopers.infrastructure.catalog.RankedProductEntry

interface MvProductRankRepository {
    fun getWeeklyRanking(page: Int, size: Int): List<RankedProductEntry>
    fun getWeeklyTotalCount(): Long
    fun getMonthlyRanking(page: Int, size: Int): List<RankedProductEntry>
    fun getMonthlyTotalCount(): Long
}
