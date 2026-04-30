package com.loopers.domain.ranking

/**
 * MV 상품 랭킹 Repository (Domain Layer).
 *
 * 배치 Job에서 MV 테이블의 적재/삭제/버전 관리를 수행한다.
 */
interface MvProductRankRepository {

    // --- Weekly ---
    fun saveAllWeekly(ranks: List<MvProductRankWeekly>)
    fun findMaxWeeklyVersion(yearWeek: String): Int?
    fun deleteWeeklyByYearWeekAndVersionLessThan(yearWeek: String, version: Int)

    // --- Monthly ---
    fun saveAllMonthly(ranks: List<MvProductRankMonthly>)
    fun findMaxMonthlyVersion(yearMonth: String): Int?
    fun deleteMonthlyByYearMonthAndVersionLessThan(yearMonth: String, version: Int)

    // --- Temp ---
    fun saveAllTemp(temps: List<RankingAggregateTemp>)
    fun findTopTempByPeriod(periodType: String, periodKey: String, limit: Int): List<RankingAggregateTemp>
    fun deleteTempByPeriod(periodType: String, periodKey: String)
}
