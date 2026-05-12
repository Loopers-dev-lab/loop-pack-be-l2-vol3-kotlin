package com.loopers.application.ranking

import com.loopers.application.brand.BrandInfo
import com.loopers.application.product.ProductInfo
import com.loopers.domain.brand.BrandService
import com.loopers.domain.product.ProductService
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.YearMonth

@Component
class ProductRankingFacade(
    private val productRankingQueryService: ProductRankingQueryService,
    private val productService: ProductService,
    private val brandService: BrandService,
) {
    data class RankingProductInfo(
        val rank: Long,
        val product: ProductInfo,
    )

    data class RankingPageInfo(
        val items: List<RankingProductInfo>,
        val page: Int,
        val size: Int,
        val totalCount: Long,
        val totalPages: Int,
    )

    fun getRankings(
        type: RankingType,
        page: Int,
        size: Int,
        date: LocalDate?,
        weekStartDate: LocalDate?,
        yearMonth: YearMonth?,
    ): RankingPageInfo {
        val rankingPage = productRankingQueryService.getRankingPage(type, page, size, date, weekStartDate, yearMonth)
        val items = rankingPage.items.mapNotNull { entry ->
            runCatching { productService.getProductById(entry.productId) }
                .getOrNull()
                ?.let { product ->
                    val brand = BrandInfo.from(brandService.getBrandById(product.brandId))
                    RankingProductInfo(rank = entry.rank, product = ProductInfo.from(product, brand))
                }
        }

        return RankingPageInfo(
            items = items,
            page = rankingPage.page,
            size = rankingPage.size,
            totalCount = rankingPage.totalCount,
            totalPages = rankingPage.totalPages,
        )
    }

    fun attachWeeklyMonthlyRanks(productInfo: ProductInfo): ProductInfo =
        productInfo.copy(
            weeklyRank = productRankingQueryService.getWeeklyRank(productInfo.id),
            monthlyRank = productRankingQueryService.getMonthlyRank(productInfo.id),
        )
}
