package com.loopers.infrastructure.queue

import com.loopers.config.redis.RedisConfig
import com.loopers.domain.queue.WaitingQueueRepository
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component

@Component
class RedisWaitingQueueRepository(
    private val redisTemplate: RedisTemplate<String, String>,
    @Qualifier(RedisConfig.REDIS_TEMPLATE_MASTER)
    private val masterRedisTemplate: RedisTemplate<String, String>,
) : WaitingQueueRepository {

    override fun enqueue(userId: Long, score: Double): Boolean {
        return masterRedisTemplate.opsForZSet()
            .addIfAbsent(KEY, userId.toString(), score) ?: false
    }

    override fun getPosition(userId: Long): Long? {
        return redisTemplate.opsForZSet().rank(KEY, userId.toString())
    }

    override fun getQueueSize(): Long {
        return redisTemplate.opsForZSet().zCard(KEY) ?: 0L
    }

    override fun dequeueTopN(count: Long): List<Long> {
        val tuples = masterRedisTemplate.opsForZSet().popMin(KEY, count) ?: return emptyList()
        return tuples.mapNotNull { it.value?.toLongOrNull() }
    }

    override fun remove(userId: Long) {
        masterRedisTemplate.opsForZSet().remove(KEY, userId.toString())
    }

    companion object {
        private const val KEY = "queue:order:waiting"
    }
}
