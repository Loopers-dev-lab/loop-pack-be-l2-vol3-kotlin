package com.loopers.application.ranking

import com.loopers.domain.brand.BrandRepository
import com.loopers.domain.product.ProductRepository
import com.loopers.domain.ranking.RankingRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GetRankingUseCase(
    private val rankingRepository: RankingRepository,
    private val productRepository: ProductRepository,
    private val brandRepository: BrandRepository,
) {
    fun getRankingPage(date: String, page: Int, size: Int): RankingPageInfo {
        val offset = ((page - 1) * size).toLong()
        val entries = rankingRepository.getTopRankings(date, offset, size.toLong())
        val totalCount = rankingRepository.getTotalCount(date)

        if (entries.isEmpty()) {
            return RankingPageInfo(
                rankings = emptyList(),
                totalCount = totalCount,
                page = page,
                size = size,
            )
        }

        val productIds = entries.map { it.productId }
        val products = productRepository.findAllByIds(productIds)
        val productMap = products.associateBy { requireNotNull(it.persistenceId) }

        val brandIds = products.map { it.refBrandId }.toSet()
        val brandMap = brandRepository.findAllByIds(brandIds)
            .associateBy { requireNotNull(it.persistenceId) }

        val rankings = entries.mapNotNull { entry ->
            val product = productMap[entry.productId] ?: return@mapNotNull null
            val brand = brandMap[product.refBrandId]
            RankingInfo(
                rank = entry.rank + 1,
                score = entry.score,
                productId = entry.productId,
                productName = product.name.value,
                price = product.price.amount,
                thumbnailUrl = product.thumbnailUrl,
                brandName = brand?.name?.value ?: "Unknown",
                likeCount = product.likeCount,
            )
        }

        return RankingPageInfo(
            rankings = rankings,
            totalCount = totalCount,
            page = page,
            size = size,
        )
    }

    fun getProductRank(date: String, productId: Long): ProductRankInfo? {
        val rank = rankingRepository.getRank(date, productId) ?: return null
        val score = rankingRepository.getScore(date, productId) ?: return null
        return ProductRankInfo(
            rank = rank + 1,
            score = score,
        )
    }
}
