package com.loopers.batch.job.ranking

import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.item.ItemProcessor
import org.springframework.stereotype.Component

@Component
class RankingChunkProcessor : ItemProcessor<RankingItem, RankedItem> {
    override fun process(item: RankingItem): RankedItem {
        // itemIndex는 0부터 시작, rank는 1부터 시작
        return RankedItem(
            productId = item.productId,
            score = item.score,
            rank = item.itemIndex + 1,
        )
    }
}

@StepScope
@Component
class MetricsAggregationProcessor : ItemProcessor<ProductMetricsDailyRow, RankingScoreContribution> {
    override fun process(item: ProductMetricsDailyRow): RankingScoreContribution {
        val score = item.viewCount * 0.1 + item.likeCount * 0.2 + item.salesCount * 0.7
        return RankingScoreContribution(
            productId = item.productId,
            score = score,
        )
    }
}

data class RankedItem(
    val productId: Long,
    val score: Double,
    val rank: Int,
)
