package com.loopers.infrastructure.product

import com.loopers.application.product.ProductInfo
import com.loopers.support.cache.CachedPage
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class ProductCacheService(
    private val redisCacheRepository: ProductCacheRepository,
    private val localCacheRepository: ProductLocalCacheRepository,
    @Value("\${cache.mode}") private val mode: String,
) {
    private val cacheMode = CacheMode.valueOf(mode.uppercase())

    fun getProductDetail(productId: Long): ProductInfo? {
        return when (cacheMode) {
            CacheMode.REDIS -> redisCacheRepository.getProductDetail(productId)
            CacheMode.LOCAL -> localCacheRepository.getProductDetail(productId)
            CacheMode.LAYERED -> {
                localCacheRepository.getProductDetail(productId)?.let { return it }
                redisCacheRepository.getProductDetail(productId)?.also {
                    localCacheRepository.setProductDetail(productId, it)
                }
            }
        }
    }

    fun setProductDetail(productId: Long, info: ProductInfo) {
        when (cacheMode) {
            CacheMode.REDIS -> redisCacheRepository.setProductDetail(productId, info)
            CacheMode.LOCAL -> localCacheRepository.setProductDetail(productId, info)
            CacheMode.LAYERED -> {
                redisCacheRepository.setProductDetail(productId, info)
                localCacheRepository.setProductDetail(productId, info)
            }
        }
    }

    fun evictProductDetail(productId: Long) {
        when (cacheMode) {
            CacheMode.REDIS -> redisCacheRepository.evictProductDetail(productId)
            CacheMode.LOCAL -> localCacheRepository.evictProductDetail(productId)
            CacheMode.LAYERED -> {
                redisCacheRepository.evictProductDetail(productId)
                localCacheRepository.evictProductDetail(productId)
            }
        }
    }

    fun getProductList(brandId: Long?, sort: String, page: Int, size: Int): CachedPage<ProductInfo>? {
        return when (cacheMode) {
            CacheMode.REDIS -> redisCacheRepository.getProductList(brandId, sort, page, size)
            CacheMode.LOCAL -> localCacheRepository.getProductList(brandId, sort, page, size)
            CacheMode.LAYERED -> {
                localCacheRepository.getProductList(brandId, sort, page, size)?.let { return it }
                redisCacheRepository.getProductList(brandId, sort, page, size)?.also {
                    localCacheRepository.setProductList(brandId, sort, page, size, it)
                }
            }
        }
    }

    fun setProductList(brandId: Long?, sort: String, page: Int, size: Int, data: CachedPage<ProductInfo>) {
        when (cacheMode) {
            CacheMode.REDIS -> redisCacheRepository.setProductList(brandId, sort, page, size, data)
            CacheMode.LOCAL -> localCacheRepository.setProductList(brandId, sort, page, size, data)
            CacheMode.LAYERED -> {
                redisCacheRepository.setProductList(brandId, sort, page, size, data)
                localCacheRepository.setProductList(brandId, sort, page, size, data)
            }
        }
    }

    fun evictAllProductLists() {
        when (cacheMode) {
            CacheMode.REDIS -> redisCacheRepository.evictAllProductLists()
            CacheMode.LOCAL -> localCacheRepository.evictAllProductLists()
            CacheMode.LAYERED -> {
                redisCacheRepository.evictAllProductLists()
                localCacheRepository.evictAllProductLists()
            }
        }
    }
}
