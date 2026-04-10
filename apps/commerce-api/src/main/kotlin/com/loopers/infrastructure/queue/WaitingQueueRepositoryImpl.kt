package com.loopers.infrastructure.queue

import com.loopers.config.redis.RedisConfig
import com.loopers.domain.queue.WaitingQueueRepository
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component

@Component
class WaitingQueueRepositoryImpl(
    @Qualifier(RedisConfig.REDIS_TEMPLATE_MASTER) private val redisTemplate: RedisTemplate<String, String>,
) : WaitingQueueRepository {

    companion object {
        private const val WAITING_QUEUE_KEY = "waiting-queue"
    }

    override fun enter(userId: Long, score: Double): Boolean {
        return redisTemplate.opsForZSet()
            .addIfAbsent(WAITING_QUEUE_KEY, userId.toString(), score) ?: false
    }

    override fun getPosition(userId: Long): Long? {
        return redisTemplate.opsForZSet()
            .rank(WAITING_QUEUE_KEY, userId.toString())
    }

    override fun getTotalWaitingCount(): Long {
        return redisTemplate.opsForZSet()
            .zCard(WAITING_QUEUE_KEY) ?: 0
    }

    override fun popMinN(count: Long): Set<String> {
        return redisTemplate.opsForZSet()
            .popMin(WAITING_QUEUE_KEY, count)
            ?.mapNotNull { it.value }
            ?.toSet()
            ?: emptySet()
    }

    override fun exists(userId: Long): Boolean {
        return redisTemplate.opsForZSet()
            .rank(WAITING_QUEUE_KEY, userId.toString()) != null
    }
}
