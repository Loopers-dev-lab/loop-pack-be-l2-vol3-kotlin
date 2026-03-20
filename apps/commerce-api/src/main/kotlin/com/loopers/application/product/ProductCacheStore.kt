package com.loopers.application.product

interface ProductCacheStore {
    fun getProduct(productId: Long): ProductInfo?
    fun putProduct(productId: Long, info: ProductInfo)
    fun evictProduct(productId: Long)
    fun getProductList(cacheKey: String): ProductListResult?
    fun putProductList(cacheKey: String, result: ProductListResult)
}
