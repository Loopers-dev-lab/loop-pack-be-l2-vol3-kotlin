package com.loopers.infrastructure.ranking

import com.loopers.domain.ranking.MvProductRankMonthly
import com.loopers.domain.ranking.MvProductRankRepository
import com.loopers.domain.ranking.MvProductRankWeekly
import com.loopers.domain.ranking.RankingAggregateTemp
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component

@Component
class MvProductRankRepositoryImpl(
    private val weeklyJpaRepository: MvProductRankWeeklyJpaRepository,
    private val monthlyJpaRepository: MvProductRankMonthlyJpaRepository,
    private val tempJpaRepository: RankingAggregateTempJpaRepository,
) : MvProductRankRepository {

    // --- Weekly ---
    override fun saveAllWeekly(ranks: List<MvProductRankWeekly>) {
        weeklyJpaRepository.saveAll(ranks)
    }

    override fun findMaxWeeklyVersion(yearWeek: String): Int? {
        return weeklyJpaRepository.findMaxVersionByYearWeek(yearWeek)
    }

    override fun deleteWeeklyByYearWeekAndVersionLessThan(yearWeek: String, version: Int) {
        weeklyJpaRepository.deleteByYearWeekAndVersionLessThan(yearWeek, version)
    }

    // --- Monthly ---
    override fun saveAllMonthly(ranks: List<MvProductRankMonthly>) {
        monthlyJpaRepository.saveAll(ranks)
    }

    override fun findMaxMonthlyVersion(yearMonth: String): Int? {
        return monthlyJpaRepository.findMaxVersionByYearMonth(yearMonth)
    }

    override fun deleteMonthlyByYearMonthAndVersionLessThan(yearMonth: String, version: Int) {
        monthlyJpaRepository.deleteByYearMonthAndVersionLessThan(yearMonth, version)
    }

    // --- Temp ---
    override fun saveAllTemp(temps: List<RankingAggregateTemp>) {
        tempJpaRepository.saveAll(temps)
    }

    override fun findTopTempByPeriod(
        periodType: String,
        periodKey: String,
        limit: Int,
    ): List<RankingAggregateTemp> {
        return tempJpaRepository.findTopByPeriod(periodType, periodKey, PageRequest.of(0, limit))
    }

    override fun deleteTempByPeriod(periodType: String, periodKey: String) {
        tempJpaRepository.deleteByPeriod(periodType, periodKey)
    }
}
