package com.loopers.application.brand

interface BrandCacheStore {
    fun getBrand(brandId: Long): BrandInfo?
    fun putBrand(brandId: Long, info: BrandInfo)
    fun evictBrand(brandId: Long)
}
