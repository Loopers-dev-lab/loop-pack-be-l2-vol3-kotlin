package com.loopers.application.product

import com.loopers.support.common.PageQuery
import com.loopers.support.common.PageResult

interface ProductLocalCacheRepository {
    fun getOrLoadProduct(productId: Long, loader: () -> ProductDetailInfo): ProductDetailInfo
    fun getOrLoadProducts(
        brandId: Long?,
        pageQuery: PageQuery,
        loader: () -> PageResult<ProductInfo>,
    ): PageResult<ProductInfo>

    fun evictProduct(productId: Long)
    fun evictAllProductLists()
    fun evictAll()
}
