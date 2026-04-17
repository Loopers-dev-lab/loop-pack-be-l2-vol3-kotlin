package com.loopers.application.ranking

import com.loopers.domain.brand.BrandService
import com.loopers.domain.product.ProductService
import com.loopers.domain.ranking.RankingService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Component
class RankingFacade(
    private val rankingService: RankingService,
    private val productService: ProductService,
    private val brandService: BrandService,
) {
    @Transactional(readOnly = true)
    fun getRankings(date: LocalDate, page: Int, size: Int): RankingPageInfo {
        val rankingPage = rankingService.getTopRankings(date, page, size)

        if (rankingPage.entries.isEmpty()) {
            return RankingPageInfo.empty(page, size)
        }

        val productIds = rankingPage.entries.map { it.productId }
        val productMap = productService.findAllByIds(productIds).associateBy { it.id }

        val brandIds = productMap.values.map { it.brandId }.distinct()
        val brandMap = brandService.findAllByIds(brandIds).associateBy { it.id }

        val content = rankingPage.entries.mapNotNull { ranked ->
            val product = productMap[ranked.productId] ?: return@mapNotNull null
            val brand = brandMap[product.brandId] ?: return@mapNotNull null
            RankingItemInfo(
                rank = ranked.rank,
                score = ranked.score,
                product = RankingProductInfo.of(product, brand),
            )
        }

        return RankingPageInfo(
            content = content,
            totalElements = rankingPage.totalElements,
            page = page,
            size = size,
        )
    }
}
