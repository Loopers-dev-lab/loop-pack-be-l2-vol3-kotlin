package com.loopers.infrastructure.ranking

import com.loopers.domain.ranking.RankingMetric
import com.loopers.domain.ranking.RankingMetricRepository
import org.springframework.stereotype.Repository

@Repository
class RankingMetricRepositoryImpl(
    private val rankingMetricJpaRepository: RankingMetricJpaRepository,
) : RankingMetricRepository {

    override fun findByProductIdAndRankingDate(productId: Long, rankingDate: String): RankingMetric? {
        return rankingMetricJpaRepository.findByProductIdAndRankingDate(productId, rankingDate)
    }

    override fun findAllByRankingDate(rankingDate: String): List<RankingMetric> {
        return rankingMetricJpaRepository.findAllByRankingDate(rankingDate)
    }

    override fun save(metric: RankingMetric): RankingMetric {
        return rankingMetricJpaRepository.save(metric)
    }

    override fun existsByRankingDate(rankingDate: String): Boolean {
        return rankingMetricJpaRepository.existsByRankingDate(rankingDate)
    }
}
