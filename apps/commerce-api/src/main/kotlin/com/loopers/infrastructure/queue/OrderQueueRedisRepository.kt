package com.loopers.infrastructure.queue

import com.loopers.config.redis.RedisConfig.Companion.REDIS_TEMPLATE_MASTER
import com.loopers.config.redis.RedisKeys
import com.loopers.domain.queue.OrderQueueRepository
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import io.github.resilience4j.retry.annotation.Retry
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.script.DefaultRedisScript
import org.springframework.stereotype.Component

@Component
class OrderQueueRedisRepository(
    @Qualifier(REDIS_TEMPLATE_MASTER)
    private val masterRedisTemplate: RedisTemplate<String, String>,
    private val readRedisTemplate: RedisTemplate<String, String>,
) : OrderQueueRepository {

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

    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "requeueFallback")
    @Retry(name = CIRCUIT_BREAKER_NAME)
    override fun requeue(userIds: List<Long>) {
        val key = RedisKeys.orderQueueKey()
        userIds.forEach { userId ->
            // 이미 대기열에 있는 유저의 score를 덮어쓰지 않기 위해 addIfAbsent 사용
            masterZSet.addIfAbsent(key, userId.toString(), 0.0)
        }
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

    internal fun requeueFallback(userIds: List<Long>, e: Exception) {
        log.error("대기열 재삽입 실패: userIds={}", userIds, e)
    }

    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "popFrontAndIssueTokensFallback")
    @Retry(name = CIRCUIT_BREAKER_NAME)
    override fun popFrontAndIssueTokens(
        count: Long,
        tokens: List<String>,
        tokenTtlSeconds: Long,
    ): Map<Long, String> {
        val args = mutableListOf(count.toString(), tokenTtlSeconds.toString(), ENTRY_TOKEN_KEY_PREFIX)
        args.addAll(tokens)

        @Suppress("UNCHECKED_CAST")
        val result = masterRedisTemplate.execute(
            POP_AND_ISSUE_SCRIPT,
            listOf(RedisKeys.orderQueueKey()),
            *args.toTypedArray(),
        ) as? List<String> ?: emptyList()

        val admitted = linkedMapOf<Long, String>()
        for (i in result.indices step 2) {
            val userId = result[i].toLong()
            val token = result[i + 1]
            admitted[userId] = token
        }
        return admitted
    }

    internal fun popFrontAndIssueTokensFallback(
        count: Long,
        tokens: List<String>,
        tokenTtlSeconds: Long,
        e: Exception,
    ): Map<Long, String> {
        log.warn("대기열 원자적 입장 처리 bypass", e)
        return emptyMap()
    }

    companion object {
        const val CIRCUIT_BREAKER_NAME = "order-queue"

        private const val ENTRY_TOKEN_KEY_PREFIX = "entry-token:"

        // ZPOPMIN으로 대기열에서 꺼내고 각 유저에게 entry-token을 원자적으로 발급하는 Lua 스크립트
        // KEYS[1] = order-queue 키
        // ARGV[1] = count, ARGV[2] = token TTL(초), ARGV[3] = entry-token 키 prefix
        // ARGV[4..] = 미리 생성된 토큰 목록
        private val POP_AND_ISSUE_SCRIPT = DefaultRedisScript<List<*>>(
            """
            local members = redis.call('ZPOPMIN', KEYS[1], tonumber(ARGV[1]))
            local ttl = tonumber(ARGV[2])
            local prefix = ARGV[3]
            local result = {}

            for i = 1, #members, 2 do
                local userId = members[i]
                local tokenIdx = math.floor((i + 1) / 2)
                local token = ARGV[3 + tokenIdx]
                if not token then break end
                redis.call('SET', prefix .. userId, token, 'EX', ttl)
                result[#result + 1] = userId
                result[#result + 1] = token
            end

            return result
            """.trimIndent(),
            List::class.java,
        )
    }
}
