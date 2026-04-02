package com.loopers.infrastructure.queue

import com.loopers.config.redis.RedisConfig
import com.loopers.domain.queue.EntryTokenRepository
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class EntryTokenRepositoryImpl(
    @Qualifier(RedisConfig.REDIS_TEMPLATE_MASTER) private val redisTemplate: RedisTemplate<String, String>,
) : EntryTokenRepository {

    companion object {
        private const val ENTRY_TOKEN_KEY_PREFIX = "entry-token:"
    }

    override fun issueToken(userId: Long, token: String, ttlSeconds: Long) {
        redisTemplate.opsForValue()
            .set("$ENTRY_TOKEN_KEY_PREFIX$userId", token, ttlSeconds, TimeUnit.SECONDS)
    }

    override fun getToken(userId: Long): String? {
        return redisTemplate.opsForValue()
            .get("$ENTRY_TOKEN_KEY_PREFIX$userId")
    }

    override fun deleteToken(userId: Long) {
        redisTemplate.delete("$ENTRY_TOKEN_KEY_PREFIX$userId")
    }

    override fun hasToken(userId: Long): Boolean {
        return redisTemplate.hasKey("$ENTRY_TOKEN_KEY_PREFIX$userId") == true
    }
}
