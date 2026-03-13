package com.loopers.infrastructure.cache

import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.script.DefaultRedisScript
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.UUID

@Component
class CacheStampedeGuard(
    private val redisTemplate: RedisTemplate<String, String>,
    private val cacheProperties: CacheProperties,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val releaseLockScript = DefaultRedisScript<Long>(
        "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
        Long::class.java,
    )

    fun <T> executeWithMutex(
        cacheKey: String,
        loader: () -> T,
        cacheWriter: (T) -> Unit,
        cacheReader: () -> T?,
    ): T {
        if (cacheProperties.stampede.strategy != StampedeStrategy.MUTEX) {
            val value = loader()
            cacheWriter(value)
            return value
        }

        val lockKey = "lock:$cacheKey"
        val token = UUID.randomUUID().toString()
        val acquired = redisTemplate.opsForValue()
            .setIfAbsent(lockKey, token, Duration.ofSeconds(5)) ?: false

        if (acquired) {
            return try {
                val value = loader()
                cacheWriter(value)
                value
            } finally {
                releaseLock(lockKey, token)
            }
        }

        // 락 획득 실패 시 재시도 (최대 3회, 100ms 간격)
        repeat(3) {
            Thread.sleep(100)
            val cached = cacheReader()
            if (cached != null) return cached
        }

        log.warn("Mutex 대기 후에도 캐시 미스: {}", cacheKey)
        val value = loader()
        cacheWriter(value)
        return value
    }

    private fun releaseLock(lockKey: String, token: String) {
        redisTemplate.execute(releaseLockScript, listOf(lockKey), token)
    }
}
