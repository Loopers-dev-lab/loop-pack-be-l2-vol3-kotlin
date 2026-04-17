package com.loopers.application.ranking

import com.loopers.application.brand.BrandCacheStore
import com.loopers.application.brand.BrandInfo
import com.loopers.application.brand.BrandService
import com.loopers.application.product.ProductCacheStore
import com.loopers.application.product.ProductInfo
import com.loopers.application.product.ProductService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Component
class RankingFacade(
    private val rankingService: RankingService,
    private val productService: ProductService,
    private val productCacheStore: ProductCacheStore,
    private val brandService: BrandService,
    private val brandCacheStore: BrandCacheStore,
) {
    @Transactional(readOnly = true)
    fun getRankings(date: String, page: Int, size: Int): RankingPageInfo {
        val pageResult = rankingService.getTopRankings(date, page, size)
        return toPageInfo(pageResult)
    }

    @Transactional(readOnly = true)
    fun getRankings(period: RankingPeriod, date: String, page: Int, size: Int): RankingPageInfo {
        if (period == RankingPeriod.DAILY) return getRankings(date, page, size)
        val localDate = LocalDate.parse(date, DATE_FORMATTER)
        val result = rankingService.getPeriodRankings(period, localDate, page, size)
        return toPageInfo(result)
    }

    @Transactional(readOnly = true)
    fun getHourlyRankings(date: String, hour: String, page: Int, size: Int): RankingPageInfo {
        val pageResult = rankingService.getHourlyTopRankings(date, hour, page, size)
        return toPageInfo(pageResult)
    }

    private fun toPageInfo(pageResult: RankingPageResult): RankingPageInfo {
        val productIds = pageResult.entries.map { it.productId }
        val productMap = fetchProducts(productIds)

        val items = pageResult.entries.mapNotNull { entry ->
            val productInfo = productMap[entry.productId] ?: return@mapNotNull null
            RankingItemInfo(
                rank = entry.rank,
                score = entry.score,
                product = productInfo,
            )
        }

        val droppedCount = pageResult.entries.size - items.size
        val filteredTotal = (pageResult.totalElements - droppedCount).coerceAtLeast(0)
        val pageSize = pageResult.entries.size.coerceAtLeast(1)
        val filteredPages = if (filteredTotal <= 0) 0 else kotlin.math.ceil(filteredTotal.toDouble() / pageSize).toInt()

        return RankingPageInfo(
            items = items,
            totalElements = filteredTotal.coerceAtLeast(0),
            totalPages = filteredPages.coerceAtLeast(0),
        )
    }

    private fun fetchProducts(productIds: List<Long>): Map<Long, ProductInfo> {
        if (productIds.isEmpty()) return emptyMap()

        val cachedMap = productIds.mapNotNull { id ->
            productCacheStore.getProduct(id)?.let { id to it }
        }.toMap()

        val uncachedIds = productIds.filter { it !in cachedMap }
        if (uncachedIds.isEmpty()) return cachedMap

        val products = productService.getProductsByIds(uncachedIds)
        val brandMap = products.map { it.brandId }.distinct()
            .associateWith { brandId -> getCachedBrandName(brandId) }

        val fetchedMap = products.associate { product ->
            val brandName = brandMap[product.brandId]
            val info = ProductInfo.from(product, brandName)
            if (brandName != null) {
                productCacheStore.putProduct(product.id, info)
            }
            product.id to info
        }

        return cachedMap + fetchedMap
    }

    private fun getCachedBrandName(brandId: Long): String? {
        brandCacheStore.getBrand(brandId)?.let { return it.name }
        val brand = runCatching { brandService.getBrand(brandId) }.getOrNull() ?: return null
        brandCacheStore.putBrand(brandId, BrandInfo.from(brand))
        return brand.name
    }

    companion object {
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd")
    }
}
