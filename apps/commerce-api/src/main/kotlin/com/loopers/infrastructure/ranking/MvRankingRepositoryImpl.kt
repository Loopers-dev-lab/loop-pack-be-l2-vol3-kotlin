package com.loopers.infrastructure.ranking

import com.loopers.domain.ranking.MvRankingRepository
import com.loopers.domain.ranking.RankingEntry
import com.loopers.domain.ranking.RankingWindow
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Repository

/**
 * MV 기반 랭킹 조회 구현체.
 *
 * 최신 완료 version 기준으로 MV 테이블을 조회한다.
 * WEEKLY → mv_product_rank_weekly, MONTHLY → mv_product_rank_monthly.
 */
@Repository
class MvRankingRepositoryImpl(
    private val weeklyJpaRepository: MvProductRankWeeklyJpaRepository,
    private val monthlyJpaRepository: MvProductRankMonthlyJpaRepository,
) : MvRankingRepository {

    override fun getTopRankings(
        window: RankingWindow,
        periodKey: String,
        offset: Long,
        limit: Long,
    ): List<RankingEntry> {
        val pageable = PageRequest.of((offset / limit).toInt(), limit.toInt())

        return when (window) {
            RankingWindow.WEEKLY -> {
                weeklyJpaRepository.findByYearWeekLatestVersion(periodKey, pageable)
                    .map { RankingEntry(productId = it.productId, score = it.score) }
            }
            RankingWindow.MONTHLY -> {
                monthlyJpaRepository.findByYearMonthLatestVersion(periodKey, pageable)
                    .map { RankingEntry(productId = it.productId, score = it.score) }
            }
            else -> emptyList()
        }
    }

    override fun getTotalCount(window: RankingWindow, periodKey: String): Long {
        return when (window) {
            RankingWindow.WEEKLY -> weeklyJpaRepository.countByYearWeekLatestVersion(periodKey)
            RankingWindow.MONTHLY -> monthlyJpaRepository.countByYearMonthLatestVersion(periodKey)
            else -> 0L
        }
    }
}
