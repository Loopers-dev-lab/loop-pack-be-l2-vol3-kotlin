package com.loopers.application.ranking

import com.loopers.domain.brand.BrandService
import com.loopers.domain.product.ProductInfo
import com.loopers.domain.product.ProductService
import com.loopers.domain.ranking.RankingService
import org.springframework.stereotype.Component

/**
 * 랭킹 Facade.
 *
 * RankingService(ZSET 읽기) + ProductService(상품정보) + BrandService(브랜드명)를
 * 조합하여 RankingPageResult를 생성한다.
 */
@Component
class RankingFacade(
    private val rankingService: RankingService,
    private val productService: ProductService,
    private val brandService: BrandService,
) {

    fun getRankings(criteria: GetRankingsCriteria): RankingPageResult {
        val rankingPage = rankingService.getTopRankings(
            window = criteria.window,
            windowKey = criteria.windowKey,
            page = criteria.page,
            size = criteria.size,
        )

        if (rankingPage.rankings.isEmpty()) {
            return RankingPageResult(
                rankings = emptyList(),
                totalCount = rankingPage.totalCount,
                page = rankingPage.page,
                size = rankingPage.size,
            )
        }

        val productIds = rankingPage.rankings.map { it.productId }
        val productMap = productService.findByIds(productIds)
            .associate { it.id to ProductInfo.from(it) }

        val brandIds = productMap.values.map { it.brandId }.distinct()
        val brandMap = brandService.findByIds(brandIds).associateBy { it.id }

        val rankedProducts = rankingPage.rankings.mapNotNull { rankingInfo ->
            val productInfo = productMap[rankingInfo.productId] ?: return@mapNotNull null
            val brandName = brandMap[productInfo.brandId]?.name ?: "알 수 없는 브랜드"
            RankedProductResult.from(rankingInfo, productInfo, brandName)
        }

        return RankingPageResult(
            rankings = rankedProducts,
            totalCount = rankingPage.totalCount,
            page = rankingPage.page,
            size = rankingPage.size,
        )
    }
}
