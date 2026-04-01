package com.loopers.infrastructure.queue

import com.loopers.config.redis.RedisConfig.Companion.REDIS_TEMPLATE_MASTER
import com.loopers.config.redis.RedisKeys
import com.loopers.domain.queue.OrderQueueRepository
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component

@Component
class OrderQueueRedisRepository(
    @Qualifier(REDIS_TEMPLATE_MASTER)
    private val masterRedisTemplate: RedisTemplate<String, String>,
    private val readRedisTemplate: RedisTemplate<String, String>,
) : OrderQueueRepository {

    private val masterZSet = masterRedisTemplate.opsForZSet()
    private val readZSet = readRedisTemplate.opsForZSet()

    override fun enqueue(userId: Long, score: Double): Boolean {
        return masterZSet.addIfAbsent(RedisKeys.orderQueueKey(), userId.toString(), score)
            ?: false
    }

    override fun getPosition(userId: Long): Long? {
        return readZSet.rank(RedisKeys.orderQueueKey(), userId.toString())
    }

    override fun getTotalSize(): Long {
        return readZSet.zCard(RedisKeys.orderQueueKey()) ?: 0L
    }

    override fun popFront(count: Long): List<Long> {
        val result = masterZSet.popMin(RedisKeys.orderQueueKey(), count) ?: emptySet()
        return result.mapNotNull { it.value?.toLongOrNull() }
    }
}
