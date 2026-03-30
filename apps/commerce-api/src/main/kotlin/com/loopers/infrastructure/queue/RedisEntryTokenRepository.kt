package com.loopers.infrastructure.queue

import com.loopers.config.redis.RedisConfig.Companion.REDIS_TEMPLATE_MASTER
import com.loopers.domain.common.vo.UserId
import com.loopers.domain.queue.token.repository.EntryTokenRepository
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Repository
import java.time.Duration

@Repository
class RedisEntryTokenRepository(
    @param:Qualifier(REDIS_TEMPLATE_MASTER)
    private val redisTemplate: RedisTemplate<String, String>,
) : EntryTokenRepository {

    companion object {
        private const val TOKEN_KEY_PREFIX = "entry-token:"
    }

    private fun tokenKey(userId: UserId) = "$TOKEN_KEY_PREFIX${userId.value}"

    override fun issue(userId: UserId, token: String, ttlSeconds: Long) {
        redisTemplate.opsForValue().set(
            tokenKey(userId),
            token,
            Duration.ofSeconds(ttlSeconds),
        )
    }

    override fun find(userId: UserId): String? {
        return redisTemplate.opsForValue().get(tokenKey(userId))
    }

    override fun delete(userId: UserId) {
        redisTemplate.delete(tokenKey(userId))
    }
}
