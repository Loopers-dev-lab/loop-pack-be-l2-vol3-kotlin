package com.loopers.batch.job.ranking.infrastructure

import com.loopers.batch.job.ranking.domain.RankingMetric
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface RankingMetricJpaRepository : JpaRepository<RankingMetric, Long> {
    fun findAllByRankingDate(rankingDate: String): List<RankingMetric>
    fun existsByRankingDate(rankingDate: String): Boolean

    @Query(
        value = """
            SELECT rm.product_id, SUM(rm.total_score) as total_score
            FROM ranking_metric rm
            WHERE rm.ranking_date BETWEEN :startDate AND :endDate
            GROUP BY rm.product_id
            ORDER BY total_score DESC
            LIMIT 100
        """,
        nativeQuery = true,
    )
    fun findTopAggregatedByDateRange(startDate: String, endDate: String): List<Array<Any>>

    fun findAllByRankingDateBetween(startDate: String, endDate: String): List<RankingMetric>
}
