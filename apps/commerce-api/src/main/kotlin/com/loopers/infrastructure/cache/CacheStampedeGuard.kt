package com.loopers.infrastructure.cache

import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.script.DefaultRedisScript
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

@Component
class CacheStampedeGuard(
    private val redisTemplate: RedisTemplate<String, String>,
    private val cacheProperties: CacheProperties,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val flights = ConcurrentHashMap<String, CompletableFuture<out Any>>()

    private val releaseLockScript = DefaultRedisScript<Long>(
        "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
        Long::class.java,
    )

    fun <T : Any> execute(
        cacheKey: String,
        loader: () -> T,
        cacheWriter: (T) -> Unit,
        cacheReader: (() -> T?)? = null,
    ): T {
        return when (cacheProperties.stampede.strategy) {
            StampedeStrategy.MUTEX -> executeWithMutex(cacheKey, loader, cacheWriter, cacheReader)
            StampedeStrategy.SINGLE_FLIGHT -> executeWithSingleFlight(cacheKey, loader, cacheWriter)
            else -> {
                val value = loader()
                cacheWriter(value)
                value
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> executeWithSingleFlight(
        cacheKey: String,
        loader: () -> T,
        cacheWriter: (T) -> Unit,
    ): T {
        val newFuture = CompletableFuture<T>()
        val existing = flights.putIfAbsent(cacheKey, newFuture)

        if (existing != null) {
            return (existing as CompletableFuture<T>).join()
        }

        return try {
            val value = loader()
            cacheWriter(value)
            newFuture.complete(value)
            value
        } catch (e: Exception) {
            newFuture.completeExceptionally(e)
            throw e
        } finally {
            flights.remove(cacheKey)
        }
    }

    private fun <T : Any> executeWithMutex(
        cacheKey: String,
        loader: () -> T,
        cacheWriter: (T) -> Unit,
        cacheReader: (() -> T?)?,
    ): T {
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

        if (cacheReader != null) {
            repeat(3) {
                Thread.sleep(100)
                val cached = cacheReader()
                if (cached != null) return cached
            }
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
