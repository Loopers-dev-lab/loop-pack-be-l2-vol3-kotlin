package com.loopers.infrastructure.cache

import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class CacheStampedeGuard(
    private val redisTemplate: RedisTemplate<String, String>,
    private val cacheProperties: CacheProperties,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun <T> executeWithMutex(
        cacheKey: String,
        loader: () -> T,
        cacheWriter: (T) -> Unit,
        cacheReader: () -> T?,
    ): T {
        if (cacheProperties.stampede.strategy != StampedeStrategy.MUTEX) {
            return loader()
        }

        val lockKey = "lock:$cacheKey"
        val acquired = redisTemplate.opsForValue()
            .setIfAbsent(lockKey, "1", Duration.ofSeconds(5)) ?: false

        if (acquired) {
            return try {
                val value = loader()
                cacheWriter(value)
                value
            } finally {
                redisTemplate.delete(lockKey)
            }
        }

        // 락 획득 실패 시 재시도 (최대 3회, 100ms 간격)
        repeat(3) {
            Thread.sleep(100)
            val cached = cacheReader()
            if (cached != null) return cached
        }

        log.warn("Mutex 대기 후에도 캐시 미스: {}", cacheKey)
        return loader()
    }
}
