package com.loopers.application.product

import com.loopers.domain.product.ProductSortType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface ProductCacheStore {
    fun getProductDetail(productId: Long): ProductCacheSnapshot?
    fun putProductDetail(product: ProductCacheSnapshot)
    fun getProductList(brandId: Long?, sortType: ProductSortType, pageable: Pageable): Page<ProductCacheSnapshot>?
    fun putProductList(brandId: Long?, sortType: ProductSortType, pageable: Pageable, products: Page<ProductCacheSnapshot>)
    fun evictProductDetail(productId: Long)
    fun evictProductList()
}
