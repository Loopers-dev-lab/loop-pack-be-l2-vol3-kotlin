package com.loopers.infrastructure.queue

import com.loopers.config.redis.RedisConfig.Companion.REDIS_TEMPLATE_MASTER
import com.loopers.config.redis.RedisKeys
import com.loopers.domain.queue.OrderQueueRepository
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import io.github.resilience4j.retry.annotation.Retry
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component

@Component
class OrderQueueRedisRepository(
    @Qualifier(REDIS_TEMPLATE_MASTER)
    private val masterRedisTemplate: RedisTemplate<String, String>,
    private val readRedisTemplate: RedisTemplate<String, String>,
) : OrderQueueRepository {

    companion object {
        const val CIRCUIT_BREAKER_NAME = "order-queue"
    }

    private val log = LoggerFactory.getLogger(javaClass)
    private val masterZSet = masterRedisTemplate.opsForZSet()
    private val readZSet = readRedisTemplate.opsForZSet()

    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "enqueueFallback")
    @Retry(name = CIRCUIT_BREAKER_NAME)
    override fun enqueue(userId: Long, score: Double): Boolean {
        return masterZSet.addIfAbsent(RedisKeys.orderQueueKey(), userId.toString(), score)
            ?: false
    }

    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "getPositionFallback")
    @Retry(name = CIRCUIT_BREAKER_NAME)
    override fun getPosition(userId: Long): Long? {
        return readZSet.rank(RedisKeys.orderQueueKey(), userId.toString())
    }

    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "getTotalSizeFallback")
    @Retry(name = CIRCUIT_BREAKER_NAME)
    override fun getTotalSize(): Long {
        return readZSet.zCard(RedisKeys.orderQueueKey()) ?: 0L
    }

    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "popFrontFallback")
    @Retry(name = CIRCUIT_BREAKER_NAME)
    override fun popFront(count: Long): List<Long> {
        val result = masterZSet.popMin(RedisKeys.orderQueueKey(), count) ?: emptySet()
        return result.mapNotNull { it.value?.toLongOrNull() }
    }

    // bypass: Redis 장애 시 대기열 없이 주문 허용 (PRD 14.3 설계 결정)
    internal fun enqueueFallback(userId: Long, score: Double, e: Exception): Boolean {
        log.warn("대기열 진입 bypass: userId={}", userId, e)
        return true
    }

    internal fun getPositionFallback(userId: Long, e: Exception): Long? {
        log.warn("대기열 순번 조회 bypass: userId={}", userId, e)
        return null
    }

    internal fun getTotalSizeFallback(e: Exception): Long {
        log.warn("대기열 크기 조회 bypass", e)
        return 0L
    }

    internal fun popFrontFallback(count: Long, e: Exception): List<Long> {
        log.warn("대기열 입장 허용 bypass", e)
        return emptyList()
    }
}
