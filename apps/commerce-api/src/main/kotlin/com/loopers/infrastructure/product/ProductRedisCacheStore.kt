package com.loopers.infrastructure.product

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.loopers.application.product.ProductCacheStore
import com.loopers.application.product.ProductInfo
import com.loopers.application.product.ProductListCache
import com.loopers.domain.product.ProductSortType
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration
import kotlin.random.Random

@Component
class ProductRedisCacheStore(
    private val redisTemplate: RedisTemplate<String, String>,
    private val objectMapper: ObjectMapper,
) : ProductCacheStore {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val DETAIL_KEY_PREFIX = "product:detail:"
        private const val LIST_VERSION_KEY = "product:list:version"
        private const val LIST_KEY_PREFIX = "product:list:"

        private val DETAIL_BASE_TTL = Duration.ofMinutes(5)
        private val LIST_BASE_TTL = Duration.ofMinutes(1)
        private val JITTER_MAX_SECONDS = 30L
    }

    // ===== Detail Cache =====

    override fun getDetail(productId: Long): ProductInfo? {
        return try {
            val json = redisTemplate.opsForValue().get("$DETAIL_KEY_PREFIX$productId")
            json?.let { objectMapper.readValue<ProductInfo>(it) }
        } catch (e: Exception) {
            log.warn("Failed to get product detail cache for id={}", productId, e)
            null
        }
    }

    override fun putDetail(productId: Long, productInfo: ProductInfo) {
        try {
            val json = objectMapper.writeValueAsString(productInfo)
            val ttl = DETAIL_BASE_TTL.plusSeconds(Random.nextLong(0, JITTER_MAX_SECONDS))
            redisTemplate.opsForValue().set("$DETAIL_KEY_PREFIX$productId", json, ttl)
        } catch (e: Exception) {
            log.warn("Failed to put product detail cache for id={}", productId, e)
        }
    }

    override fun evictDetail(productId: Long) {
        try {
            redisTemplate.delete("$DETAIL_KEY_PREFIX$productId")
        } catch (e: Exception) {
            log.warn("Failed to evict product detail cache for id={}", productId, e)
        }
    }

    override fun getDetails(productIds: List<Long>): Map<Long, ProductInfo> {
        if (productIds.isEmpty()) return emptyMap()
        return try {
            val keys = productIds.map { "$DETAIL_KEY_PREFIX$it" }
            val values = redisTemplate.opsForValue().multiGet(keys) ?: return emptyMap()

            productIds.zip(values)
                .filter { (_, json) -> json != null }
                .associate { (id, json) -> id to objectMapper.readValue<ProductInfo>(json!!) }
        } catch (e: Exception) {
            log.warn("Failed to batch get product detail caches", e)
            emptyMap()
        }
    }

    // ===== List Cache (version-based invalidation) =====

    override fun getListIds(brandId: Long?, sort: ProductSortType, page: Int): ProductListCache? {
        return try {
            val version = getListVersion()
            val key = buildListKey(version, brandId, sort, page)
            val json = redisTemplate.opsForValue().get(key)
            json?.let { objectMapper.readValue<ProductListCache>(it) }
        } catch (e: Exception) {
            log.warn("Failed to get product list cache", e)
            null
        }
    }

    override fun putListIds(brandId: Long?, sort: ProductSortType, page: Int, cache: ProductListCache) {
        try {
            val version = getListVersion()
            val key = buildListKey(version, brandId, sort, page)
            val json = objectMapper.writeValueAsString(cache)
            val ttl = LIST_BASE_TTL.plusSeconds(Random.nextLong(0, JITTER_MAX_SECONDS))
            redisTemplate.opsForValue().set(key, json, ttl)
        } catch (e: Exception) {
            log.warn("Failed to put product list cache", e)
        }
    }

    override fun evictAllLists() {
        try {
            redisTemplate.opsForValue().increment(LIST_VERSION_KEY)
        } catch (e: Exception) {
            log.warn("Failed to increment product list cache version", e)
        }
    }

    private fun getListVersion(): Long {
        return try {
            redisTemplate.opsForValue().get(LIST_VERSION_KEY)?.toLongOrNull() ?: 0L
        } catch (e: Exception) {
            log.warn("Failed to get product list cache version", e)
            0L
        }
    }

    private fun buildListKey(version: Long, brandId: Long?, sort: ProductSortType, page: Int): String {
        val brandPart = brandId?.toString() ?: "all"
        return "${LIST_KEY_PREFIX}v$version:$brandPart:${sort.name}:$page"
    }
}
