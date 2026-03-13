package com.loopers.application.brand

class FakeBrandCacheStore : BrandCacheStore {
    private val cache = mutableMapOf<Long, BrandInfo>()

    override fun getBrand(brandId: Long): BrandInfo? {
        return cache[brandId]
    }

    override fun putBrand(brandId: Long, info: BrandInfo) {
        cache[brandId] = info
    }

    override fun evictBrand(brandId: Long) {
        cache.remove(brandId)
    }

    fun clear() {
        cache.clear()
    }
}
