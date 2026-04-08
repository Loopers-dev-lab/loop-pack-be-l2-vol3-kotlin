package com.loopers.infrastructure.queue

import com.loopers.application.queue.QueueStrategyType
import com.loopers.config.redis.RedisConfig
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component

@Component
class QueueSequenceAllocator(
    @Qualifier(RedisConfig.REDIS_TEMPLATE_MASTER)
    private val redisTemplate: RedisTemplate<String, String>,
) {
    fun next(strategyType: QueueStrategyType): Long {
        return redisTemplate.opsForValue().increment("queue:${strategyType.name.lowercase()}:db-sequence") ?: 0L
    }
}
