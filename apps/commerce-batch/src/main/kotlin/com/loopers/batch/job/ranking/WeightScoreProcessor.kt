package com.loopers.batch.job.ranking

import org.springframework.batch.item.ItemProcessor

class WeightScoreProcessor(
    private val properties: RankingWeightProperties,
    private val periodKey: String,
) : ItemProcessor<ProductAggregateDto, ProductRankRow> {

    override fun process(item: ProductAggregateDto): ProductRankRow {
        val score = item.viewCount * properties.viewWeight +
            item.likeCount * properties.likeWeight +
            item.salesCount * properties.salesWeight

        return ProductRankRow(
            periodKey = periodKey,
            productId = item.productId,
            score = score,
            viewCount = item.viewCount,
            likeCount = item.likeCount,
            salesCount = item.salesCount,
        )
    }
}
