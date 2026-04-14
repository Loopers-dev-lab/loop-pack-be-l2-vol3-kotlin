package com.loopers.infrastructure.ranking

import com.loopers.domain.ranking.RankingMetric
import org.springframework.data.jpa.repository.JpaRepository

interface RankingMetricJpaRepository : JpaRepository<RankingMetric, Long> {
    fun findByProductIdAndRankingDate(productId: Long, rankingDate: String): RankingMetric?
    fun findAllByRankingDate(rankingDate: String): List<RankingMetric>
    fun existsByRankingDate(rankingDate: String): Boolean
}
