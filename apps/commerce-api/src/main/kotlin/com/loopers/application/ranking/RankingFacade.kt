package com.loopers.application.ranking

import com.loopers.domain.brand.BrandService
import com.loopers.domain.product.ProductService
import com.loopers.domain.ranking.RankingEntry
import com.loopers.domain.ranking.RankingPeriod
import com.loopers.domain.ranking.RankingService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class RankingFacade(
    private val rankingService: RankingService,
    private val productService: ProductService,
    private val brandService: BrandService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun getRanking(period: RankingPeriod, date: String, page: Int, size: Int): List<RankingProductInfo> {
        val entries = when (period) {
            RankingPeriod.DAILY -> rankingService.getDailyRanking(date, page, size)
            RankingPeriod.WEEKLY -> rankingService.getWeeklyRanking(date, page, size)
            RankingPeriod.MONTHLY -> rankingService.getMonthlyRanking(date, page, size)
        }
        if (entries.isEmpty()) return emptyList()

        return enrichWithProductInfo(entries, page, size)
    }

    fun getRankingTotalCount(period: RankingPeriod, date: String): Long {
        return when (period) {
            RankingPeriod.DAILY -> rankingService.getDailyTotalCount(date)
            RankingPeriod.WEEKLY -> rankingService.getWeeklyTotalCount(date)
            RankingPeriod.MONTHLY -> rankingService.getMonthlyTotalCount(date)
        }
    }

    private fun enrichWithProductInfo(
        entries: List<RankingEntry>,
        page: Int,
        size: Int,
    ): List<RankingProductInfo> {
        val productIds = entries.map { it.productId }
        val products = productService.getProductsByIds(productIds).associateBy { it.id }

        val brandIds = products.values.map { it.brandId }.distinct()
        val brands = brandService.getBrandsByIds(brandIds).associateBy { it.id }

        return entries.mapIndexedNotNull { index, entry ->
            val product = products[entry.productId] ?: return@mapIndexedNotNull null
            val brand = brands[product.brandId] ?: return@mapIndexedNotNull null
            RankingProductInfo(
                rank = ((page - 1) * size + index + 1).toLong(),
                score = entry.score,
                productId = entry.productId,
                productName = product.name,
                productPrice = product.price,
                brandId = brand.id,
                brandName = brand.name,
            )
        }
    }
}
