package com.loopers.batch.job.ranking.infrastructure

import com.loopers.batch.job.ranking.domain.RankingMetric
import org.springframework.data.jpa.repository.JpaRepository

interface RankingMetricJpaRepository : JpaRepository<RankingMetric, Long> {
    fun findAllByRankingDate(rankingDate: String): List<RankingMetric>
    fun existsByRankingDate(rankingDate: String): Boolean
}
