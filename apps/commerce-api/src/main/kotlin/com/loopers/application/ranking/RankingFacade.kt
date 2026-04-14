package com.loopers.application.ranking

import com.loopers.domain.brand.BrandService
import com.loopers.domain.product.ProductService
import com.loopers.domain.ranking.RankingService
import org.springframework.stereotype.Component

@Component
class RankingFacade(
    private val rankingService: RankingService,
    private val productService: ProductService,
    private val brandService: BrandService,
) {
    fun getRanking(date: String, page: Int, size: Int): List<RankingProductInfo> {
        val entries = rankingService.getRanking(date, page, size)
        if (entries.isEmpty()) return emptyList()

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

    fun getRankingTotalCount(date: String): Long {
        return rankingService.getTotalCount(date)
    }
}
