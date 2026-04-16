package com.loopers.batch.job.ranking.step

import com.loopers.batch.job.ranking.domain.RankingAggregation
import com.loopers.batch.job.ranking.infrastructure.RankingMetricJpaRepository
import org.slf4j.LoggerFactory
import org.springframework.batch.item.ItemReader

class RankingAggregationReader(
    private val rankingMetricJpaRepository: RankingMetricJpaRepository,
    private val startDate: String,
    private val endDate: String,
) : ItemReader<RankingAggregation> {
    private val log = LoggerFactory.getLogger(javaClass)
    private var items: MutableList<RankingAggregation>? = null
    private var index = 0

    override fun read(): RankingAggregation? {
        if (items == null) {
            items = rankingMetricJpaRepository.findTopAggregatedByDateRange(startDate, endDate)
                .map { row ->
                    RankingAggregation(
                        productId = (row[0] as Number).toLong(),
                        totalScore = (row[1] as Number).toDouble(),
                    )
                }.toMutableList()
            log.info("[RankingReader] Loaded {} aggregated items for period {}-{}", items!!.size, startDate, endDate)
        }

        return if (index < items!!.size) {
            items!![index++]
        } else {
            null
        }
    }
}
