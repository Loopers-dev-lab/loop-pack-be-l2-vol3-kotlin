package com.loopers.infrastructure.queue

import com.loopers.config.redis.RedisConfig
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.script.DefaultRedisScript
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.UUID

@Component
class RedisDistributedLockExecutor(
    @Qualifier(RedisConfig.REDIS_TEMPLATE_MASTER)
    private val redisTemplate: RedisTemplate<String, String>,
) {
    fun <T> execute(key: String, ttl: Duration, action: () -> T): T? {
        val ownerToken = UUID.randomUUID().toString()
        val acquired = redisTemplate.opsForValue().setIfAbsent(key, ownerToken, ttl) == true
        if (!acquired) {
            return null
        }

        return try {
            action()
        } finally {
            release(key, ownerToken)
        }
    }

    private fun release(key: String, ownerToken: String) {
        val script = DefaultRedisScript<Long>()
        script.setScriptText(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
        )
        script.resultType = Long::class.java
        redisTemplate.execute(script, listOf(key), ownerToken)
    }
}
