package com.loopers.application.product

import com.loopers.domain.product.ProductSortType

interface ProductCachePort {
    fun getProductDetail(id: Long): ProductInfo?
    fun setProductDetail(id: Long, productInfo: ProductInfo)
    fun evictProductDetail(id: Long)

    fun getProductList(brandId: Long?, sortType: ProductSortType): List<ProductInfo>?
    fun setProductList(brandId: Long?, sortType: ProductSortType, list: List<ProductInfo>)
}
