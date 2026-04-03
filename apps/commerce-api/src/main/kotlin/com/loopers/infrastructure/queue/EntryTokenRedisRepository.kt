package com.loopers.infrastructure.queue

import com.loopers.config.redis.RedisConfig.Companion.REDIS_TEMPLATE_MASTER
import com.loopers.config.redis.RedisKeys
import com.loopers.domain.queue.EntryTokenRepository
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import io.github.resilience4j.retry.annotation.Retry
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.RedisTemplate
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
    override fun issue(userId: Long, token: String, ttlSeconds: Long) {
        masterValue.set(RedisKeys.entryTokenKey(userId), token, ttlSeconds, TimeUnit.SECONDS)
    }

    @CircuitBreaker(name = OrderQueueRedisRepository.CIRCUIT_BREAKER_NAME, fallbackMethod = "getFallback")
    @Retry(name = OrderQueueRedisRepository.CIRCUIT_BREAKER_NAME)
    override fun get(userId: Long): String? {
        return masterValue.get(RedisKeys.entryTokenKey(userId))
    }

    @CircuitBreaker(name = OrderQueueRedisRepository.CIRCUIT_BREAKER_NAME, fallbackMethod = "consumeFallback")
    @Retry(name = OrderQueueRedisRepository.CIRCUIT_BREAKER_NAME)
    override fun consume(userId: Long) {
        masterRedisTemplate.delete(RedisKeys.entryTokenKey(userId))
    }

    internal fun getFallback(userId: Long, e: Exception): String? {
        log.warn("토큰 조회 bypass: userId={}", userId, e)
        return EntryTokenRepository.BYPASS_TOKEN
    }

    internal fun issueFallback(userId: Long, token: String, ttlSeconds: Long, e: Exception) {
        log.warn("토큰 발급 bypass: userId={}", userId, e)
    }

    internal fun consumeFallback(userId: Long, e: Exception) {
        log.warn("토큰 소비 bypass: userId={}", userId, e)
    }
}
