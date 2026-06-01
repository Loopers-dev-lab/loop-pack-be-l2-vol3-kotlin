package com.loopers.batch.job.ranking.step

import com.loopers.domain.ranking.ProductRankAggregation
import com.loopers.domain.ranking.ProductRankResult
import org.springframework.batch.item.ItemProcessor
import java.util.concurrent.atomic.AtomicInteger

class RankingAggregationProcessor : ItemProcessor<ProductRankAggregation, ProductRankResult> {

    private val currentRank = AtomicInteger(0)

    override fun process(item: ProductRankAggregation): ProductRankResult {
        return ProductRankResult(
            productId = item.productId,
            totalScore = item.totalScore,
            viewCount = item.viewCount,
            likeCount = item.likeCount,
            orderCount = item.orderCount,
            rank = currentRank.incrementAndGet(),
        )
    }
}
