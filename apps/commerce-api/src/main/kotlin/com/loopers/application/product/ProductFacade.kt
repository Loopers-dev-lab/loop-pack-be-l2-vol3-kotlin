package com.loopers.application.product

import com.loopers.application.brand.BrandCacheStore
import com.loopers.application.brand.BrandInfo
import com.loopers.application.brand.BrandService
import com.loopers.application.ranking.RankingService
import com.loopers.application.useraction.LogUserAction
import com.loopers.domain.product.ProductSearchCondition
import com.loopers.domain.product.ProductSort
import com.loopers.domain.useraction.UserActionTargetType
import com.loopers.domain.useraction.UserActionType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Component
class ProductFacade(
    private val productService: ProductService,
    private val brandService: BrandService,
    private val productCacheStore: ProductCacheStore,
    private val brandCacheStore: BrandCacheStore,
    private val rankingService: RankingService,
) {
    @LogUserAction(action = UserActionType.VIEW, targetType = UserActionTargetType.PRODUCT)
    @Transactional(readOnly = true)
    fun getProduct(productId: Long): ProductInfo {
        val info = productCacheStore.getProduct(productId)
            ?: run {
                val product = productService.getProduct(productId)
                val brandName = getCachedBrandName(product.brandId)
                val productInfo = ProductInfo.from(product, brandName)
                productCacheStore.putProduct(productId, productInfo)
                productInfo
            }

        val todayDate = LocalDate.now(ZONE_ID).format(DAILY_FORMATTER)
        val position = rankingService.getRank(todayDate, productId)
        return info.copy(rank = position?.rank)
    }

    @Transactional(readOnly = true)
    fun getProducts(brandId: Long?, sort: ProductSort, size: Int, cursor: String?): ProductListResult {
        val cacheKey = buildListCacheKey(brandId, sort, size, cursor)
        productCacheStore.getProductList(cacheKey)?.let { return it }

        val condition = ProductSearchCondition(
            brandId = brandId,
            sort = sort,
            size = size,
            cursor = cursor,
        )

        val cursorResult = productService.getProducts(condition)

        val brandIds = cursorResult.content.map { it.brandId }.distinct()
        val brandMap = brandIds.associateWith { id -> getCachedBrandName(id) }

        val items = cursorResult.content.map { ProductInfo.from(it, brandMap[it.brandId]) }

        val result = ProductListResult(
            data = items,
            nextCursor = cursorResult.nextCursor,
            hasNext = cursorResult.hasNext,
        )
        productCacheStore.putProductList(cacheKey, result)
        return result
    }

    private fun getCachedBrandName(brandId: Long): String? {
        brandCacheStore.getBrand(brandId)?.let { return it.name }
        val brand = runCatching { brandService.getBrand(brandId) }.getOrNull() ?: return null
        brandCacheStore.putBrand(brandId, BrandInfo.from(brand))
        return brand.name
    }

    private fun buildListCacheKey(brandId: Long?, sort: ProductSort, size: Int, cursor: String?): String {
        val brand = brandId?.toString() ?: "all"
        val cursorPart = cursor ?: "first"
        return "$brand:${sort.name}:$size:$cursorPart"
    }

    companion object {
        private val ZONE_ID = ZoneId.of("Asia/Seoul")
        private val DAILY_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd")
    }
}

data class ProductListResult(
    val data: List<ProductInfo>,
    val nextCursor: String?,
    val hasNext: Boolean,
)
