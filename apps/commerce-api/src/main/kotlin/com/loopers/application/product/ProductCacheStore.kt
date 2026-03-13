package com.loopers.application.product

import com.loopers.domain.product.ProductSortType

interface ProductCacheStore {
    fun getDetail(productId: Long, loader: () -> ProductInfo.Detail): ProductInfo.Detail

    fun getList(
        sortType: ProductSortType,
        brandId: Long?,
        loader: () -> List<ProductInfo.Main>,
    ): List<ProductInfo.Main>

    fun evictDetail(productId: Long)

    fun evictAllDetails()

    fun evictList(brandId: Long? = null)
}
