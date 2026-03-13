package com.loopers.application.product

import com.loopers.support.common.PageQuery
import com.loopers.support.common.PageResult

interface ProductCacheRepository {
    fun getProduct(productId: Long): ProductDetailInfo?
    fun setProduct(productId: Long, productDetailInfo: ProductDetailInfo)
    fun evictProduct(productId: Long)

    fun getProducts(brandId: Long?, pageQuery: PageQuery): PageResult<ProductInfo>?
    fun setProducts(brandId: Long?, pageQuery: PageQuery, pageResult: PageResult<ProductInfo>)
    fun evictAllProducts()
}
