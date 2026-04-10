package com.loopers.application.ranking

import com.loopers.domain.brand.BrandRepository
import com.loopers.domain.product.ProductRepository
import com.loopers.domain.ranking.RankingRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Component
class RankingService(
    private val rankingRepository: RankingRepository,
    private val productRepository: ProductRepository,
    private val brandRepository: BrandRepository,
) {

    @Transactional(readOnly = true)
    fun getRankings(date: LocalDate, page: Int, size: Int): RankingPageInfo {
        val start = ((page - 1) * size).toLong()
        val end = start + size - 1

        val rankingScores = rankingRepository.getTopRankings(date, start, end)
        val totalCount = rankingRepository.getTotalCount(date)

        if (rankingScores.isEmpty()) {
            return RankingPageInfo(rankings = emptyList(), totalCount = totalCount)
        }

        val productIds = rankingScores.map { it.productId }
        val productMap = productRepository.findAllByIds(productIds).associateBy { it.id }

        val brandIds = productMap.values.map { it.brandId }.distinct()
        val brandMap = brandRepository.findAllByIdIncludingDeleted(brandIds).associateBy { it.id }

        val rankings = rankingScores
            .mapIndexedNotNull { index, score ->
                val product = productMap[score.productId] ?: return@mapIndexedNotNull null
                val brand = brandMap[product.brandId]
                RankingInfo(
                    rank = (start + index + 1).toInt(),
                    productId = product.id,
                    productName = product.name,
                    brandName = brand?.name,
                    price = product.price,
                    imageUrl = product.imageUrl,
                    score = score.score,
                )
            }

        return RankingPageInfo(rankings = rankings, totalCount = totalCount)
    }

    fun getProductRank(productId: Long, date: LocalDate): Int? {
        val rank = rankingRepository.getProductRank(productId, date) ?: return null
        return (rank + 1).toInt()
    }
}

data class RankingPageInfo(
    val rankings: List<RankingInfo>,
    val totalCount: Long,
)
