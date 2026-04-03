package com.loopers.infrastructure.queue

import com.loopers.config.redis.RedisConfig
import com.loopers.domain.queue.EntryToken
import com.loopers.domain.queue.EntryTokenRepository
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.script.RedisScript
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.UUID
import java.util.concurrent.TimeUnit

@Component
class RedisEntryTokenRepository(
    @Qualifier(RedisConfig.REDIS_TEMPLATE_MASTER)
    private val redisTemplate: RedisTemplate<String, String>,
) : EntryTokenRepository {
    override fun issue(userId: Long): EntryToken {
        val token = UUID.randomUUID().toString()
        val key = tokenKey(userId)
        redisTemplate.opsForValue().set(key, token, TOKEN_TTL)
        return EntryToken(token = token, userId = userId, remainingSeconds = TOKEN_TTL.seconds)
    }

    override fun validate(
        userId: Long,
        token: String,
    ): Boolean {
        val stored = redisTemplate.opsForValue().get(tokenKey(userId)) ?: return false
        return stored == token
    }

    override fun validateAndConsume(
        userId: Long,
        token: String,
    ): Boolean {
        val result =
            redisTemplate.execute(
                VALIDATE_AND_CONSUME_SCRIPT,
                listOf(tokenKey(userId)),
                token,
            )
        return result == 1L
    }

    override fun exists(userId: Long): Boolean = redisTemplate.hasKey(tokenKey(userId)) == true

    override fun findByUserId(userId: Long): EntryToken? {
        val key = tokenKey(userId)
        val token = redisTemplate.opsForValue().get(key) ?: return null
        val ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS)
        if (ttl <= 0) return null
        return EntryToken(token = token, userId = userId, remainingSeconds = ttl)
    }

    companion object {
        private val TOKEN_TTL = Duration.ofSeconds(300)

        private fun tokenKey(userId: Long): String = "queue:entry-token:$userId"

        private val VALIDATE_AND_CONSUME_SCRIPT =
            RedisScript.of<Long>(
                """
                local key = KEYS[1]
                local expectedToken = ARGV[1]
                local storedToken = redis.call('GET', key)
                if storedToken == expectedToken then
                    redis.call('DEL', key)
                    return 1
                end
                return 0
                """.trimIndent(),
                Long::class.javaObjectType,
            )
    }
}
