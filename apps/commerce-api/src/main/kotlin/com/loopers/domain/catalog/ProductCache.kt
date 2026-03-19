package com.loopers.domain.catalog

import org.springframework.data.domain.Slice

interface ProductCache {

    fun getProduct(id: Long, loader: () -> ProductInfo): ProductInfo

    fun searchProducts(
        sortType: ProductSortType,
        brandId: Long?,
        page: Int,
        size: Int,
        loader: () -> Slice<ProductInfo>,
    ): Slice<ProductInfo>

    fun evictProduct(productId: Long)

    fun evictProductList()

    fun evictPopularList()
}
