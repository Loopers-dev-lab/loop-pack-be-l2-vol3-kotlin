package com.loopers.infrastructure.product

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.loopers.application.product.ProductCacheStore
import com.loopers.application.product.ProductInfo
import com.loopers.support.PageResult
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ScanOptions
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
        private const val LIST_KEY_PREFIX = "product:list:"

        private val DETAIL_BASE_TTL = Duration.ofMinutes(5)
        private val LIST_BASE_TTL = Duration.ofMinutes(1)
        private val JITTER_MAX_SECONDS = 30L
    }

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

    override fun getList(brandId: Long?, sort: String, page: Int, size: Int): PageResult<ProductInfo>? {
        return try {
            val key = buildListKey(brandId, sort, page, size)
            val json = redisTemplate.opsForValue().get(key)
            json?.let { objectMapper.readValue<PageResult<ProductInfo>>(it) }
        } catch (e: Exception) {
            log.warn("Failed to get product list cache", e)
            null
        }
    }

    override fun putList(brandId: Long?, sort: String, page: Int, size: Int, result: PageResult<ProductInfo>) {
        try {
            val key = buildListKey(brandId, sort, page, size)
            val json = objectMapper.writeValueAsString(result)
            val ttl = LIST_BASE_TTL.plusSeconds(Random.nextLong(0, JITTER_MAX_SECONDS))
            redisTemplate.opsForValue().set(key, json, ttl)
        } catch (e: Exception) {
            log.warn("Failed to put product list cache", e)
        }
    }

    override fun evictAllLists() {
        try {
            val keys = mutableSetOf<String>()
            val scanOptions = ScanOptions.scanOptions().match("$LIST_KEY_PREFIX*").count(100).build()
            redisTemplate.scan(scanOptions).use { cursor ->
                cursor.forEach { keys.add(it) }
            }
            if (keys.isNotEmpty()) {
                redisTemplate.delete(keys)
            }
        } catch (e: Exception) {
            log.warn("Failed to evict product list caches", e)
        }
    }

    private fun buildListKey(brandId: Long?, sort: String, page: Int, size: Int): String {
        val brandPart = brandId?.toString() ?: "all"
        return "$LIST_KEY_PREFIX$brandPart:$sort:$page:$size"
    }
}
