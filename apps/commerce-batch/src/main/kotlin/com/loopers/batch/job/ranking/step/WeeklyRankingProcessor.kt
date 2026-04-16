package com.loopers.batch.job.ranking.step

import com.loopers.batch.job.ranking.domain.ProductRankWeekly
import com.loopers.batch.job.ranking.domain.RankingAggregation
import org.springframework.batch.item.ItemProcessor
import java.util.concurrent.atomic.AtomicInteger

class WeeklyRankingProcessor(
    private val periodDate: String,
) : ItemProcessor<RankingAggregation, ProductRankWeekly> {
    private val rankCounter = AtomicInteger(0)

    override fun process(item: RankingAggregation): ProductRankWeekly {
        return ProductRankWeekly(
            productId = item.productId,
            rankingRank = rankCounter.incrementAndGet(),
            totalScore = item.totalScore,
            periodDate = periodDate,
        )
    }
}
