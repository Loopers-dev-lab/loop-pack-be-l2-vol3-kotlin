package com.loopers.batch.job.ranking.step

import com.loopers.batch.job.ranking.domain.RankingAggregation
import com.loopers.batch.job.ranking.infrastructure.RankingMetricJpaRepository
import org.slf4j.LoggerFactory
import org.springframework.batch.item.ItemReader

class AppAggregationReader(
    private val rankingMetricJpaRepository: RankingMetricJpaRepository,
    private val startDate: String,
    private val endDate: String,
) : ItemReader<RankingAggregation> {
    private val log = LoggerFactory.getLogger(javaClass)
    private var items: MutableList<RankingAggregation>? = null
    private var index = 0

    override fun read(): RankingAggregation? {
        if (items == null) {
            val rawMetrics = rankingMetricJpaRepository.findAllByRankingDateBetween(startDate, endDate)
            log.info("[AppAggregationReader] Loaded {} raw metrics for period {}-{}", rawMetrics.size, startDate, endDate)

            items = rawMetrics
                .groupBy { it.productId }
                .map { (productId, metrics) ->
                    RankingAggregation(
                        productId = productId,
                        totalScore = metrics.sumOf { it.totalScore },
                    )
                }
                .sortedByDescending { it.totalScore }
                .take(100)
                .toMutableList()

            log.info("[AppAggregationReader] Aggregated to {} products (TOP 100)", items!!.size)
        }

        return if (index < items!!.size) {
            items!![index++]
        } else {
            null
        }
    }
}
