package com.loopers.application.product

import com.loopers.application.brand.BrandCacheStore
import com.loopers.application.brand.BrandInfo
import com.loopers.application.brand.BrandService
import com.loopers.domain.product.ProductSearchCondition
import com.loopers.domain.product.ProductSort
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class ProductFacade(
    private val productService: ProductService,
    private val brandService: BrandService,
    private val productCacheStore: ProductCacheStore,
    private val brandCacheStore: BrandCacheStore,
) {
    @Transactional(readOnly = true)
    fun getProduct(productId: Long): ProductInfo {
        productCacheStore.getProduct(productId)?.let { return it }

        val product = productService.getProduct(productId)
        val brandName = getCachedBrandName(product.brandId)
        val info = ProductInfo.from(product, brandName)
        productCacheStore.putProduct(productId, info)
        return info
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
}

data class ProductListResult(
    val data: List<ProductInfo>,
    val nextCursor: String?,
    val hasNext: Boolean,
)
