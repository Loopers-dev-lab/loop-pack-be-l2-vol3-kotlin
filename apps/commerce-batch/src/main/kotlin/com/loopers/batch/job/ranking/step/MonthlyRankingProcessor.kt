package com.loopers.batch.job.ranking.step

import com.loopers.batch.job.ranking.domain.ProductRankMonthly
import com.loopers.batch.job.ranking.domain.RankingAggregation
import org.springframework.batch.item.ItemProcessor
import java.util.concurrent.atomic.AtomicInteger

class MonthlyRankingProcessor(
    private val periodDate: String,
) : ItemProcessor<RankingAggregation, ProductRankMonthly> {
    private val rankCounter = AtomicInteger(0)

    override fun process(item: RankingAggregation): ProductRankMonthly {
        return ProductRankMonthly(
            productId = item.productId,
            rankingRank = rankCounter.incrementAndGet(),
            totalScore = item.totalScore,
            periodDate = periodDate,
        )
    }
}
