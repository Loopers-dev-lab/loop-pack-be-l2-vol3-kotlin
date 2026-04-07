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

    private val enterScript: RedisScript<Long> = RedisScript.of(
        ClassPathResource("redis/queue-enter.lua"),
        Long::class.java,
    )

    private val tokenIssueScript: RedisScript<Long> = RedisScript.of(
        ClassPathResource("redis/queue-token-issue.lua"),
        Long::class.java,
    )

    /**
     * @return 순번 (0-based), -1이면 이미 대기열에 있음, -2이면 대기열 초과
     */
    override fun enqueue(userId: Long, maxQueueSize: Long): Long {
        return redisTemplate.execute(
            enterScript,
            listOf(queueKey, "$queueKey:counter"),
            userId.toString(),
            maxQueueSize.toString(),
        ) ?: -2L
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
