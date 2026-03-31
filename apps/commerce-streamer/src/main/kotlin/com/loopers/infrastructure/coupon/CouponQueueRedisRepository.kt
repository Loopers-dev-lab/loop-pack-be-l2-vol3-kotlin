package com.loopers.infrastructure.coupon

import com.loopers.config.redis.RedisConfig
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component

@Component
class CouponQueueRedisRepository(
    @Qualifier(RedisConfig.REDIS_TEMPLATE_MASTER)
    private val redisTemplate: RedisTemplate<String, String>,
) {
    companion object {
        private const val QUEUE_KEY_PREFIX = "coupon:queue"
        private const val ACTIVE_QUEUES_KEY = "coupon:active-queues"
    }

    fun getActiveCouponIds(): Set<String> {
        return redisTemplate.opsForSet().members(ACTIVE_QUEUES_KEY) ?: emptySet()
    }

    fun dequeue(couponId: Long, count: Long): Set<String> {
        return redisTemplate.opsForZSet()
            .popMin("$QUEUE_KEY_PREFIX:$couponId", count)
            ?.map { it.value!! }
            ?.toSet()
            ?: emptySet()
    }
}
