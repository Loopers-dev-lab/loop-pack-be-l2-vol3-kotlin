package com.loopers.infrastructure.product

import com.fasterxml.jackson.databind.ObjectMapper
import com.loopers.application.product.ProductCacheStore
import com.loopers.application.product.ProductInfo
import com.loopers.application.product.ProductListResult
import com.loopers.config.redis.RedisConfig
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class ProductCacheStoreImpl(
    private val redisTemplate: RedisTemplate<String, String>,
    @Qualifier(RedisConfig.REDIS_TEMPLATE_MASTER)
    private val masterRedisTemplate: RedisTemplate<String, String>,
    private val objectMapper: ObjectMapper,
) : ProductCacheStore {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val PRODUCT_KEY_PREFIX = "product:detail:"
        private const val PRODUCT_LIST_KEY_PREFIX = "product:list:"
        private val PRODUCT_TTL = Duration.ofMinutes(5)
        private val PRODUCT_LIST_TTL = Duration.ofMinutes(1)
    }

    override fun getProduct(productId: Long): ProductInfo? {
        return try {
            val json = redisTemplate.opsForValue().get("$PRODUCT_KEY_PREFIX$productId") ?: return null
            objectMapper.readValue(json, ProductInfo::class.java)
        } catch (e: Exception) {
            log.warn("[ProductCache] 상품 상세 캐시 조회 실패 (productId={})", productId, e)
            null
        }
    }

    override fun putProduct(productId: Long, info: ProductInfo) {
        try {
            masterRedisTemplate.opsForValue().set(
                "$PRODUCT_KEY_PREFIX$productId",
                objectMapper.writeValueAsString(info),
                PRODUCT_TTL,
            )
        } catch (e: Exception) {
            log.warn("[ProductCache] 상품 상세 캐시 저장 실패 (productId={})", productId, e)
        }
    }

    override fun evictProduct(productId: Long) {
        try {
            masterRedisTemplate.delete("$PRODUCT_KEY_PREFIX$productId")
        } catch (e: Exception) {
            log.warn("[ProductCache] 상품 상세 캐시 삭제 실패 (productId={})", productId, e)
        }
    }

    override fun getProductList(cacheKey: String): ProductListResult? {
        return try {
            val json = redisTemplate.opsForValue().get("$PRODUCT_LIST_KEY_PREFIX$cacheKey") ?: return null
            objectMapper.readValue(json, ProductListResult::class.java)
        } catch (e: Exception) {
            log.warn("[ProductCache] 상품 목록 캐시 조회 실패 (key={})", cacheKey, e)
            null
        }
    }

    override fun putProductList(cacheKey: String, result: ProductListResult) {
        try {
            masterRedisTemplate.opsForValue().set(
                "$PRODUCT_LIST_KEY_PREFIX$cacheKey",
                objectMapper.writeValueAsString(result),
                PRODUCT_LIST_TTL,
            )
        } catch (e: Exception) {
            log.warn("[ProductCache] 상품 목록 캐시 저장 실패 (key={})", cacheKey, e)
        }
    }
}
