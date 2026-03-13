package com.loopers.application.product

import com.loopers.domain.product.ProductSortType

sealed class ProductDetailCache {
    data class Hit(val productInfo: ProductInfo) : ProductDetailCache()
    data object NotExist : ProductDetailCache()
    data object Miss : ProductDetailCache()
}

interface ProductCacheStore {

    fun getDetail(productId: Long): ProductDetailCache

    fun putDetail(productId: Long, productInfo: ProductInfo)

    fun putNullDetail(productId: Long)

    fun evictDetail(productId: Long)

    fun getListVersion(): Long

    fun getListIds(version: Long, brandId: Long?, sort: ProductSortType, page: Int): ProductListCache?

    fun putListIds(version: Long, brandId: Long?, sort: ProductSortType, page: Int, cache: ProductListCache)

    fun evictAllLists()

    fun getDetails(productIds: List<Long>): Map<Long, ProductInfo>

    companion object {
        const val MAX_CACHEABLE_PAGE = 2
    }
}
