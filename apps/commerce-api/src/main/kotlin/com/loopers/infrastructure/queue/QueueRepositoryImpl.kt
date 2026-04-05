package com.loopers.infrastructure.queue

import com.loopers.config.redis.RedisConfig
import com.loopers.domain.queue.QueueRepository
import com.loopers.domain.queue.QueuedUser
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class QueueRepositoryImpl(
    @Qualifier(RedisConfig.REDIS_TEMPLATE_MASTER)
    private val redisTemplate: RedisTemplate<String, String>,
) : QueueRepository {

    companion object {
        private const val QUEUE_KEY_PREFIX = "queue:"
        private const val TOKEN_KEY_PREFIX = "entry-token:"
    }

    private fun key(queueName: String) = "$QUEUE_KEY_PREFIX$queueName"
    private fun tokenKey(queueName: String, userId: Long) = "$TOKEN_KEY_PREFIX$queueName:$userId"

    override fun enter(queueName: String, userId: Long, score: Double): Boolean {
        val ops = redisTemplate.opsForZSet()
        return ops.add(key(queueName), userId.toString(), score) ?: false
    }

    override fun getRank(queueName: String, userId: Long): Long? {
        return redisTemplate.opsForZSet().rank(key(queueName), userId.toString())
    }

    override fun size(queueName: String): Long {
        return redisTemplate.opsForZSet().size(key(queueName)) ?: 0L
    }

    override fun remove(queueName: String, userId: Long) {
        redisTemplate.opsForZSet().remove(key(queueName), userId.toString())
    }

    override fun popMin(queueName: String, count: Long): List<QueuedUser> {
        return redisTemplate.opsForZSet()
            .popMin(key(queueName), count)
            ?.mapNotNull { tuple ->
                tuple?.value?.toLongOrNull()?.let { userId ->
                    QueuedUser(userId, tuple.score ?: 0.0)
                }
            }
            ?.toList()
            ?: emptyList()
    }

    override fun issueToken(queueName: String, userId: Long, token: String, ttlSeconds: Long) {
        redisTemplate.opsForValue().set(tokenKey(queueName, userId), token, Duration.ofSeconds(ttlSeconds))
    }

    override fun getToken(queueName: String, userId: Long): String? {
        return redisTemplate.opsForValue().get(tokenKey(queueName, userId))
    }

    override fun getAndConsume(queueName: String, userId: Long): String? {
        return redisTemplate.opsForValue().getAndDelete(tokenKey(queueName, userId))
    }

    override fun deleteToken(queueName: String, userId: Long) {
        redisTemplate.delete(tokenKey(queueName, userId))
    }
}
