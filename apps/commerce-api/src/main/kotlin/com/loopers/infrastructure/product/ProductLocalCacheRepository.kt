package com.loopers.infrastructure.product

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.loopers.application.product.ProductInfo
import com.loopers.support.cache.CachedPage
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class ProductLocalCacheRepository {

    private val log = LoggerFactory.getLogger(javaClass)

    private val detailCache: Cache<Long, ProductInfo> = Caffeine.newBuilder()
        .maximumSize(10_000)
        .expireAfterWrite(Duration.ofSeconds(10))
        .recordStats()
        .build()

    private val listCache: Cache<String, CachedPage<ProductInfo>> = Caffeine.newBuilder()
        .maximumSize(500)
        .expireAfterWrite(Duration.ofSeconds(10))
        .recordStats()
        .build()

    fun getProductDetail(productId: Long): ProductInfo? = detailCache.getIfPresent(productId)

    fun setProductDetail(productId: Long, info: ProductInfo) = detailCache.put(productId, info)

    fun evictProductDetail(productId: Long) = detailCache.invalidate(productId)

    fun getProductList(brandId: Long?, sort: String, page: Int, size: Int): CachedPage<ProductInfo>? {
        return listCache.getIfPresent(buildKey(brandId, sort, page, size))
    }

    fun setProductList(brandId: Long?, sort: String, page: Int, size: Int, data: CachedPage<ProductInfo>) {
        listCache.put(buildKey(brandId, sort, page, size), data)
    }

    fun evictAllProductLists() = listCache.invalidateAll()

    fun logStats() {
        log.info("Detail cache: {}", detailCache.stats())
        log.info("List cache: {}", listCache.stats())
    }

    private fun buildKey(brandId: Long?, sort: String, page: Int, size: Int): String {
        return "${brandId ?: "all"}:$sort:$page:$size"
    }
}
