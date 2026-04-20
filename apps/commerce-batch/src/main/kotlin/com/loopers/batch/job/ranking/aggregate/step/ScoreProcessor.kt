package com.loopers.batch.job.ranking.aggregate.step

import com.loopers.batch.job.ranking.aggregate.ProductMetricRow
import com.loopers.batch.job.ranking.aggregate.ProductRankRow
import kotlin.math.log10
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.item.ItemProcessor
import org.springframework.stereotype.Component

@Component
@StepScope
class ScoreProcessor : ItemProcessor<ProductMetricRow, ProductRankRow> {

    override fun process(item: ProductMetricRow): ProductRankRow {
        val score = item.viewCount * 0.1 +
            item.likeCount * 0.2 +
            0.7 * log10(item.orderAmountSum + 1.0)
        return ProductRankRow(
            productId = item.productId,
            score = score,
            viewCount = item.viewCount,
            likeCount = item.likeCount,
            orderCount = item.orderCount,
            orderAmountSum = item.orderAmountSum,
        )
    }
}
