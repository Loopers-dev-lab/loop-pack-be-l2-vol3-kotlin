package com.loopers.batch.job.ranking.monthly

import com.loopers.batch.job.ranking.weekly.ProductAggregateDto
import com.loopers.batch.job.ranking.weekly.RankingWeightProperties
import org.springframework.batch.item.ItemProcessor

class MonthlyWeightScoreProcessor(
    private val properties: RankingWeightProperties,
    private val yearMonth: String,
) : ItemProcessor<ProductAggregateDto, ProductRankMonthlyRow> {

    override fun process(item: ProductAggregateDto): ProductRankMonthlyRow {
        val score = item.viewCount * properties.viewWeight +
            item.likeCount * properties.likeWeight +
            item.salesCount * properties.salesWeight

        return ProductRankMonthlyRow(
            yearMonth = yearMonth,
            productId = item.productId,
            score = score,
            viewCount = item.viewCount,
            likeCount = item.likeCount,
            salesCount = item.salesCount,
        )
    }
}
