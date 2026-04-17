package com.loopers.application.ranking

import com.loopers.domain.brand.BrandReader
import com.loopers.domain.product.ProductReader
import com.loopers.infrastructure.ranking.RankedProductScore
import com.loopers.infrastructure.ranking.RankingMaterializedViewReader
import com.loopers.infrastructure.ranking.RankingRedisReader
import org.springframework.stereotype.Component

@Component
class RankingUseCase(
    private val rankingRedisReader: RankingRedisReader,
    private val rankingMaterializedViewReader: RankingMaterializedViewReader,
    private val productReader: ProductReader,
    private val brandReader: BrandReader,
) {
    fun getPage(
        date: String,
        period: RankingPeriod,
        size: Int,
        page: Int,
    ): List<RankingInfo.RankedProduct> {
        require(size > 0) { "size must be positive" }
        require(page > 0) { "page must be positive" }

        val rankedScores = when (period) {
            RankingPeriod.DAILY -> {
                val start = ((page - 1) * size).toLong()
                val end = start + size - 1
                rankingRedisReader.getPage(date, start, end)
            }

            RankingPeriod.WEEKLY,
            RankingPeriod.MONTHLY,
            -> rankingMaterializedViewReader.getPage(period, date, size, page)
        }

        if (rankedScores.isEmpty()) {
            return emptyList()
        }

        val productMap = productReader.getAllByIds(rankedScores.map(RankedProductScore::productId)).associateBy { it.id }
        val brandMap = brandReader.getAllByIds(productMap.values.map { it.brandId }.distinct()).associateBy { it.id }

        return rankedScores.mapNotNull { rankedScore ->
            val product = productMap[rankedScore.productId] ?: return@mapNotNull null
            val brand = brandMap[product.brandId] ?: return@mapNotNull null
            RankingInfo.RankedProduct(
                rank = rankedScore.rank,
                productId = rankedScore.productId,
                score = rankedScore.score,
                brandId = product.brandId,
                brandName = brand.name.value,
                name = product.name.value,
                price = product.price.value,
                stock = product.stock.value,
                status = product.status.name,
                likeCount = product.likeCount,
            )
        }
    }
}
