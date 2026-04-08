package com.loopers.infrastructure.queue

import com.loopers.config.redis.RedisConfig
import com.loopers.domain.queue.QueueRepository
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component

@Component
class QueueRedisRepository(
    private val redisTemplate: RedisTemplate<String, String>,
    @Qualifier(RedisConfig.REDIS_TEMPLATE_MASTER)
    private val masterRedisTemplate: RedisTemplate<String, String>,
) : QueueRepository {
    companion object {
        private const val QUEUE_KEY = "queue:waiting"
    }

    override fun addIfAbsent(userId: Long, score: Double): Boolean {
        return masterRedisTemplate.opsForZSet().addIfAbsent(QUEUE_KEY, userId.toString(), score) ?: false
    }

    override fun getRank(userId: Long): Long? {
        return redisTemplate.opsForZSet().rank(QUEUE_KEY, userId.toString())
    }

    override fun getSize(): Long {
        return redisTemplate.opsForZSet().size(QUEUE_KEY) ?: 0L
    }

    override fun popMin(count: Long): Set<String> {
        val typedTuples = masterRedisTemplate.opsForZSet().popMin(QUEUE_KEY, count)
        return typedTuples?.mapNotNull { it.value }?.toSet() ?: emptySet()
    }

    override fun remove(userId: Long): Boolean {
        val removed = masterRedisTemplate.opsForZSet().remove(QUEUE_KEY, userId.toString())
        return removed != null && removed > 0
    }
}
