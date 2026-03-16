package com.loopers.domain.catalog.product.repository

import com.loopers.domain.catalog.product.model.Product
import com.loopers.domain.common.vo.BrandId
import com.loopers.domain.common.vo.ProductId

interface ProductCacheRepository {
    fun findProductDetail(productId: ProductId): Product?
    fun saveProductDetail(product: Product)
    fun saveProductDetailIfAbsent(product: Product)
    fun evictProductDetail(productId: ProductId)
    fun evictProductList(brandId: BrandId?)
}
