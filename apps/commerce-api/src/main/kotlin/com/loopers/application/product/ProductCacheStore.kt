package com.loopers.application.product

import com.loopers.domain.product.ProductSortType

interface ProductCacheStore {

    fun getDetail(productId: Long): ProductInfo?

    fun putDetail(productId: Long, productInfo: ProductInfo)

    fun evictDetail(productId: Long)

    fun getListIds(brandId: Long?, sort: ProductSortType, page: Int): ProductListCache?

    fun putListIds(brandId: Long?, sort: ProductSortType, page: Int, cache: ProductListCache)

    fun evictAllLists()

    fun getDetails(productIds: List<Long>): Map<Long, ProductInfo>

    companion object {
        const val MAX_CACHEABLE_PAGE = 2
    }
}
