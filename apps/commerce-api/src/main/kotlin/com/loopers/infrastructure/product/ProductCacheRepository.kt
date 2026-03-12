package com.loopers.infrastructure.product

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.loopers.application.product.ProductInfo
import com.loopers.support.cache.CachedPage
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class ProductCacheRepository(
    private val redisTemplate: RedisTemplate<String, String>,
    private val objectMapper: ObjectMapper,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val DETAIL_KEY_PREFIX = "product:detail:"
        private const val LIST_KEY_PREFIX = "product:list:"
        private val DETAIL_TTL = Duration.ofMinutes(10)
        private val LIST_TTL = Duration.ofMinutes(5)
    }

    fun getProductDetail(productId: Long): ProductInfo? {
        return try {
            val json = redisTemplate.opsForValue().get("$DETAIL_KEY_PREFIX$productId") ?: return null
            objectMapper.readValue<ProductInfo>(json)
        } catch (e: Exception) {
            log.warn("Redis 캐시 조회 실패 - product:detail:{}", productId, e)
            null
        }
    }

    fun setProductDetail(productId: Long, info: ProductInfo) {
        try {
            val json = objectMapper.writeValueAsString(info)
            redisTemplate.opsForValue().set("$DETAIL_KEY_PREFIX$productId", json, DETAIL_TTL)
        } catch (e: Exception) {
            log.warn("Redis 캐시 저장 실패 - product:detail:{}", productId, e)
        }
    }

    fun evictProductDetail(productId: Long) {
        try {
            redisTemplate.delete("$DETAIL_KEY_PREFIX$productId")
        } catch (e: Exception) {
            log.warn("Redis 캐시 삭제 실패 - product:detail:{}", productId, e)
        }
    }

    fun getProductList(brandId: Long?, sort: String, page: Int, size: Int): CachedPage<ProductInfo>? {
        return try {
            val key = buildListKey(brandId, sort, page, size)
            val json = redisTemplate.opsForValue().get(key) ?: return null
            objectMapper.readValue<CachedPage<ProductInfo>>(json)
        } catch (e: Exception) {
            log.warn("Redis 캐시 조회 실패 - product:list", e)
            null
        }
    }

    fun setProductList(brandId: Long?, sort: String, page: Int, size: Int, data: CachedPage<ProductInfo>) {
        try {
            val key = buildListKey(brandId, sort, page, size)
            val json = objectMapper.writeValueAsString(data)
            redisTemplate.opsForValue().set(key, json, LIST_TTL)
        } catch (e: Exception) {
            log.warn("Redis 캐시 저장 실패 - product:list", e)
        }
    }

    fun evictAllProductLists() {
        try {
            val keys = redisTemplate.keys("$LIST_KEY_PREFIX*")
            if (keys.isNotEmpty()) {
                redisTemplate.delete(keys)
            }
        } catch (e: Exception) {
            log.warn("Redis 캐시 삭제 실패 - product:list:*", e)
        }
    }

    private fun buildListKey(brandId: Long?, sort: String, page: Int, size: Int): String {
        val brandKey = brandId?.toString() ?: "all"
        return "$LIST_KEY_PREFIX$brandKey:$sort:$page:$size"
    }
}
