package com.loopers.domain.ranking

interface MvProductRankRepository {
    fun findWeeklyRanking(yearWeek: String, page: Int, size: Int): List<ProductRankingReadModel>
    fun findMonthlyRanking(yearMonth: String, page: Int, size: Int): List<ProductRankingReadModel>
    fun countWeekly(yearWeek: String): Long
    fun countMonthly(yearMonth: String): Long
}
