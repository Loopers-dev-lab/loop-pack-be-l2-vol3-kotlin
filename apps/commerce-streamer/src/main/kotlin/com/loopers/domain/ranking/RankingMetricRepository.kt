package com.loopers.domain.ranking

interface RankingMetricRepository {
    fun findByProductIdAndRankingDate(productId: Long, rankingDate: String): RankingMetric?
    fun findAllByRankingDate(rankingDate: String): List<RankingMetric>
    fun save(metric: RankingMetric): RankingMetric
    fun existsByRankingDate(rankingDate: String): Boolean
}
