package com.loopers.infrastructure.queue

import com.loopers.config.redis.RedisConfig.Companion.REDIS_TEMPLATE_MASTER
import com.loopers.config.redis.RedisKeys
import com.loopers.domain.queue.EntryTokenRepository
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import io.github.resilience4j.retry.annotation.Retry
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.script.DefaultRedisScript
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class EntryTokenRedisRepository(
    @Qualifier(REDIS_TEMPLATE_MASTER)
    private val masterRedisTemplate: RedisTemplate<String, String>,
) : EntryTokenRepository {

    private val log = LoggerFactory.getLogger(javaClass)
    private val masterValue = masterRedisTemplate.opsForValue()

    @CircuitBreaker(name = OrderQueueRedisRepository.CIRCUIT_BREAKER_NAME, fallbackMethod = "issueFallback")
    @Retry(name = OrderQueueRedisRepository.CIRCUIT_BREAKER_NAME)
    override fun issue(userId: Long, token: String, ttlSeconds: Long): Boolean {
        masterValue.set(RedisKeys.entryTokenKey(userId), token, ttlSeconds, TimeUnit.SECONDS)
        return true
    }

    @CircuitBreaker(name = OrderQueueRedisRepository.CIRCUIT_BREAKER_NAME, fallbackMethod = "getFallback")
    @Retry(name = OrderQueueRedisRepository.CIRCUIT_BREAKER_NAME)
    override fun get(userId: Long): String? {
        return masterValue.get(RedisKeys.entryTokenKey(userId))
    }

    @CircuitBreaker(name = OrderQueueRedisRepository.CIRCUIT_BREAKER_NAME, fallbackMethod = "consumeIfMatchesFallback")
    @Retry(name = OrderQueueRedisRepository.CIRCUIT_BREAKER_NAME)
    override fun consumeIfMatches(userId: Long, token: String): Boolean {
        val result = masterRedisTemplate.execute(
            CONSUME_IF_MATCHES_SCRIPT,
            listOf(RedisKeys.entryTokenKey(userId)),
            token,
        )
        return result == 1L
    }

    internal fun getFallback(userId: Long, e: Exception): String? {
        log.warn("토큰 조회 실패 (Redis 장애): userId={}", userId, e)
        return null
    }

    internal fun issueFallback(userId: Long, token: String, ttlSeconds: Long, e: Exception): Boolean {
        log.warn("토큰 발급 실패: userId={}", userId, e)
        return false
    }

    internal fun consumeIfMatchesFallback(userId: Long, token: String, e: Exception): Boolean {
        log.warn("토큰 소비 실패 (Redis 장애): userId={}", userId, e)
        return false
    }

    companion object {
        // 저장된 토큰과 일치하면 삭제하고 1 반환, 불일치 또는 미존재 시 0 반환
        private val CONSUME_IF_MATCHES_SCRIPT = DefaultRedisScript<Long>(
            """
            if redis.call('GET', KEYS[1]) == ARGV[1] then
                redis.call('DEL', KEYS[1])
                return 1
            end
            return 0
            """.trimIndent(),
            Long::class.javaObjectType,
        )
    }
}
