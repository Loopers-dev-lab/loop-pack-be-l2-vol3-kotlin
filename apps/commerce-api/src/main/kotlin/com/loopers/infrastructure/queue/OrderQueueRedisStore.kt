package com.loopers.infrastructure.queue

import com.loopers.config.redis.RedisConfig
import com.loopers.domain.queue.OrderQueueStore
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.ClassPathResource
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.script.RedisScript
import org.springframework.stereotype.Component

@Component
class OrderQueueRedisStore(
    @Qualifier(RedisConfig.REDIS_TEMPLATE_MASTER)
    private val redisTemplate: RedisTemplate<String, String>,
    @Value("\${queue.redis.queue-key}") private val queueKey: String,
    @Value("\${queue.redis.token-key-prefix}") private val tokenKeyPrefix: String,
) : OrderQueueStore {

    private val tokenIssueScript: RedisScript<Long> = RedisScript.of(
        ClassPathResource("redis/queue-token-issue.lua"),
        Long::class.java,
    )

    override fun enqueue(userId: Long, score: Double): Boolean {
        return redisTemplate.opsForZSet().addIfAbsent(queueKey, userId.toString(), score) ?: false
    }

    override fun getPosition(userId: Long): Long? {
        return redisTemplate.opsForZSet().rank(queueKey, userId.toString())
    }

    override fun getQueueSize(): Long {
        return redisTemplate.opsForZSet().size(queueKey) ?: 0L
    }

    override fun hasToken(userId: Long): Boolean {
        return redisTemplate.hasKey(tokenKeyPrefix + userId) ?: false
    }

    override fun consumeToken(userId: Long): Boolean {
        return redisTemplate.opsForValue().getAndDelete(tokenKeyPrefix + userId) != null
    }

    override fun deleteToken(userId: Long) {
        redisTemplate.delete(tokenKeyPrefix + userId)
    }

    override fun issueTokens(batchSize: Long, ttlSeconds: Long): Long {
        return redisTemplate.execute(
            tokenIssueScript,
            listOf(queueKey, tokenKeyPrefix),
            batchSize.toString(),
            ttlSeconds.toString(),
        ) ?: 0L
    }
}
