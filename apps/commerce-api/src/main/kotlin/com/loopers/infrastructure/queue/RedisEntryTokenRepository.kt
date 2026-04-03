package com.loopers.infrastructure.queue

import com.loopers.config.redis.RedisConfig
import com.loopers.domain.queue.EntryTokenRepository
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class RedisEntryTokenRepository(
    private val redisTemplate: RedisTemplate<String, String>,
    @Qualifier(RedisConfig.REDIS_TEMPLATE_MASTER)
    private val masterRedisTemplate: RedisTemplate<String, String>,
) : EntryTokenRepository {

    override fun issueToken(userId: Long, token: String, ttlSeconds: Long) {
        masterRedisTemplate.opsForValue()
            .set(tokenKey(userId), token, Duration.ofSeconds(ttlSeconds))
    }

    override fun findToken(userId: Long): String? {
        return redisTemplate.opsForValue().get(tokenKey(userId))
    }

    override fun deleteToken(userId: Long) {
        masterRedisTemplate.delete(tokenKey(userId))
    }

    override fun hasToken(userId: Long): Boolean {
        return redisTemplate.hasKey(tokenKey(userId))
    }

    private fun tokenKey(userId: Long) = "$KEY_PREFIX$userId"

    companion object {
        private const val KEY_PREFIX = "queue:order:token:"
    }
}
