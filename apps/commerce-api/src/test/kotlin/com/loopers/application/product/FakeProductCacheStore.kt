package com.loopers.application.product

class FakeProductCacheStore : ProductCacheStore {
    private val productCache = mutableMapOf<Long, ProductInfo>()
    private val listCache = mutableMapOf<String, ProductListResult>()

    var getProductCallCount = 0
        private set
    var putProductCallCount = 0
        private set
    var evictProductCallCount = 0
        private set
    var getProductListCallCount = 0
        private set
    var putProductListCallCount = 0
        private set

    override fun getProduct(productId: Long): ProductInfo? {
        getProductCallCount++
        return productCache[productId]
    }

    override fun putProduct(productId: Long, info: ProductInfo) {
        putProductCallCount++
        productCache[productId] = info
    }

    override fun evictProduct(productId: Long) {
        evictProductCallCount++
        productCache.remove(productId)
    }

    override fun getProductList(cacheKey: String): ProductListResult? {
        getProductListCallCount++
        return listCache[cacheKey]
    }

    override fun putProductList(cacheKey: String, result: ProductListResult) {
        putProductListCallCount++
        listCache[cacheKey] = result
    }

    fun seedProduct(productId: Long, info: ProductInfo) {
        productCache[productId] = info
    }

    fun seedProductList(cacheKey: String, result: ProductListResult) {
        listCache[cacheKey] = result
    }

    fun clear() {
        productCache.clear()
        listCache.clear()
        getProductCallCount = 0
        putProductCallCount = 0
        evictProductCallCount = 0
        getProductListCallCount = 0
        putProductListCallCount = 0
    }
}
