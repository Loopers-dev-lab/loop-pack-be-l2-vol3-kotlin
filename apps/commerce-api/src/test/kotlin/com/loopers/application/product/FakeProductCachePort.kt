package com.loopers.application.product

import com.loopers.domain.product.ProductSortType

class FakeProductCachePort : ProductCachePort {

    private val detailCache = mutableMapOf<Long, ProductInfo>()
    private val listCache = mutableMapOf<String, List<ProductInfo>>()

    override fun getProductDetail(id: Long): ProductInfo? = detailCache[id]

    override fun setProductDetail(id: Long, productInfo: ProductInfo) {
        detailCache[id] = productInfo
    }

    override fun evictProductDetail(id: Long) {
        detailCache.remove(id)
    }

    override fun getProductList(brandId: Long?, sortType: ProductSortType): List<ProductInfo>? {
        return listCache[listKey(brandId, sortType)]
    }

    override fun setProductList(brandId: Long?, sortType: ProductSortType, list: List<ProductInfo>) {
        listCache[listKey(brandId, sortType)] = list
    }

    private fun listKey(brandId: Long?, sortType: ProductSortType): String {
        val brand = brandId?.toString() ?: "all"
        return "$brand:${sortType.name}"
    }
}
