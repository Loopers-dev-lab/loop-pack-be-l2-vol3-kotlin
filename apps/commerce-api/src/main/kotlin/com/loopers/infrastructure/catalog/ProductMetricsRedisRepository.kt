package com.loopers.infrastructure.catalog

import com.loopers.config.redis.RedisConfig
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component

@Component
class ProductMetricsRedisRepository(
    @Qualifier(RedisConfig.REDIS_TEMPLATE_MASTER)
    private val redisTemplate: RedisTemplate<String, String>,
) {
    companion object {
        private const val KEY_PREFIX = "product:metrics"
    }

    fun incrementViewCount(productId: Long) {
        redisTemplate.opsForHash<String, String>().increment(key(productId), "viewCount", 1)
    }

    fun incrementLikeCount(productId: Long) {
        redisTemplate.opsForHash<String, String>().increment(key(productId), "likeCount", 1)
    }

    fun decrementLikeCount(productId: Long) {
        redisTemplate.opsForHash<String, String>().increment(key(productId), "likeCount", -1)
    }

    fun incrementOrderCount(productId: Long) {
        redisTemplate.opsForHash<String, String>().increment(key(productId), "orderCount", 1)
    }

    private fun key(productId: Long): String = "$KEY_PREFIX:$productId"
}
