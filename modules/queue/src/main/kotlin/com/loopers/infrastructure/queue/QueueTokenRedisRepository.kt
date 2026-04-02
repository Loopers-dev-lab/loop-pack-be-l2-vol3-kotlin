package com.loopers.infrastructure.queue

import com.loopers.config.redis.RedisConfig
import com.loopers.domain.queue.QueueTokenRepository
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ScanOptions
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.UUID

@Component
class QueueTokenRedisRepository(
    private val redisTemplate: RedisTemplate<String, String>,
    @Qualifier(RedisConfig.REDIS_TEMPLATE_MASTER)
    private val masterRedisTemplate: RedisTemplate<String, String>,
) : QueueTokenRepository {
    companion object {
        private const val TOKEN_KEY_PREFIX = "queue:token"
        private const val TOKEN_COUNT_KEY = "queue:token:count"
    }

    override fun issueToken(userId: Long, ttlSeconds: Long): String {
        val token = UUID.randomUUID().toString()
        masterRedisTemplate.opsForValue().set(buildKey(userId), token, Duration.ofSeconds(ttlSeconds))
        return token
    }

    override fun getToken(userId: Long): String? {
        return redisTemplate.opsForValue().get(buildKey(userId))
    }

    override fun deleteToken(userId: Long): Boolean {
        return masterRedisTemplate.delete(buildKey(userId))
    }

    override fun hasToken(userId: Long): Boolean {
        return redisTemplate.hasKey(buildKey(userId))
    }

    override fun countActiveTokens(): Long {
        var count = 0L
        val scanOptions = ScanOptions.scanOptions()
            .match("$TOKEN_KEY_PREFIX:*")
            .count(100)
            .build()
        redisTemplate.connectionFactory?.connection?.use { connection ->
            val cursor = connection.keyCommands().scan(scanOptions)
            while (cursor.hasNext()) {
                cursor.next()
                count++
            }
        }
        // count key 자체는 토큰이 아니므로 제외
        if (count > 0 && redisTemplate.hasKey(TOKEN_COUNT_KEY)) {
            count--
        }
        return count
    }

    override fun getActiveTokenCount(): Long {
        return redisTemplate.opsForValue().get(TOKEN_COUNT_KEY)?.toLongOrNull() ?: 0L
    }

    override fun setActiveTokenCount(count: Long) {
        masterRedisTemplate.opsForValue().set(TOKEN_COUNT_KEY, count.toString())
    }

    override fun incrementActiveTokenCount(delta: Long): Long {
        return masterRedisTemplate.opsForValue().increment(TOKEN_COUNT_KEY, delta) ?: 0L
    }

    override fun decrementActiveTokenCount(): Long {
        val result = masterRedisTemplate.opsForValue().decrement(TOKEN_COUNT_KEY) ?: 0L
        if (result < 0) {
            masterRedisTemplate.opsForValue().set(TOKEN_COUNT_KEY, "0")
            return 0L
        }
        return result
    }

    private fun buildKey(userId: Long): String = "$TOKEN_KEY_PREFIX:$userId"
}
