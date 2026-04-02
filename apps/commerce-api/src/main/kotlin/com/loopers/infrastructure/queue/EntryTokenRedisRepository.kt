package com.loopers.infrastructure.queue

import com.loopers.config.redis.RedisConfig.Companion.REDIS_TEMPLATE_MASTER
import com.loopers.config.redis.RedisKeys
import com.loopers.domain.queue.EntryTokenRepository
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class EntryTokenRedisRepository(
    @Qualifier(REDIS_TEMPLATE_MASTER)
    private val masterRedisTemplate: RedisTemplate<String, String>,
) : EntryTokenRepository {

    private val masterValue = masterRedisTemplate.opsForValue()

    override fun issue(userId: Long, token: String, ttlSeconds: Long) {
        masterValue.set(RedisKeys.entryTokenKey(userId), token, ttlSeconds, TimeUnit.SECONDS)
    }

    override fun get(userId: Long): String? {
        return masterValue.get(RedisKeys.entryTokenKey(userId))
    }

    override fun consume(userId: Long) {
        masterRedisTemplate.delete(RedisKeys.entryTokenKey(userId))
    }
}
