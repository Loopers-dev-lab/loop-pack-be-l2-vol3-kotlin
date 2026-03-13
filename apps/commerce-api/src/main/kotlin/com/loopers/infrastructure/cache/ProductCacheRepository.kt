package com.loopers.infrastructure.cache

import com.fasterxml.jackson.databind.ObjectMapper
import com.loopers.config.redis.RedisConfig
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class ProductCacheRepository(
    private val redisTemplate: RedisTemplate<String, String>,
    @Qualifier(RedisConfig.REDIS_TEMPLATE_MASTER)
    private val masterRedisTemplate: RedisTemplate<String, String>,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val KEY_PREFIX = "products:detail"
        private val TTL = Duration.ofSeconds(60)
    }

    fun get(productId: Long): ProductCacheDto? {
        return try {
            val key = buildKey(productId)
            val value = redisTemplate.opsForValue().get(key) ?: return null
            objectMapper.readValue(value, ProductCacheDto::class.java)
        } catch (e: Exception) {
            log.warn("[Cache] Failed to get product cache for id={}: {}", productId, e.message)
            null
        }
    }

    fun put(productId: Long, data: ProductCacheDto) {
        try {
            val key = buildKey(productId)
            val value = objectMapper.writeValueAsString(data)
            masterRedisTemplate.opsForValue().set(key, value, TTL)
        } catch (e: Exception) {
            log.warn("[Cache] Failed to put product cache for id={}: {}", productId, e.message)
        }
    }

    fun evict(productId: Long) {
        try {
            val key = buildKey(productId)
            masterRedisTemplate.delete(key)
        } catch (e: Exception) {
            log.warn("[Cache] Failed to evict product cache for id={}: {}", productId, e.message)
        }
    }

    private fun buildKey(productId: Long): String {
        return "$KEY_PREFIX:$productId"
    }
}
