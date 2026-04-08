package com.loopers.infrastructure.coupon

import com.loopers.config.redis.RedisConfig
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Repository

@Repository
class CouponCounterRedisRepository(
    @Qualifier(RedisConfig.REDIS_TEMPLATE_MASTER)
    private val redisTemplate: RedisTemplate<String, String>,
) {
    companion object {
        private const val KEY_PREFIX = "coupon:remaining"
    }

    fun initCounter(templateId: Long, count: Int) {
        redisTemplate.opsForValue().set(buildKey(templateId), count.toString())
    }

    fun decrementAndGet(templateId: Long): Long {
        return redisTemplate.opsForValue().decrement(buildKey(templateId)) ?: -1
    }

    fun increment(templateId: Long) {
        redisTemplate.opsForValue().increment(buildKey(templateId))
    }

    fun getRemaining(templateId: Long): Long {
        return redisTemplate.opsForValue().get(buildKey(templateId))?.toLong() ?: -1
    }

    private fun buildKey(templateId: Long) = "$KEY_PREFIX:$templateId"
}
