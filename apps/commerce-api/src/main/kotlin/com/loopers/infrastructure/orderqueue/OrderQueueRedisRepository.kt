package com.loopers.infrastructure.orderqueue

import com.loopers.config.redis.RedisConfig
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.script.DefaultRedisScript
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class OrderQueueRedisRepository(
    @Qualifier(RedisConfig.REDIS_TEMPLATE_MASTER)
    private val redisTemplate: RedisTemplate<String, String>,
) {
    companion object {
        private const val QUEUE_KEY = "order:queue"
        private const val COUNTER_KEY = "order:queue:counter"
        private const val TOKEN_KEY_PREFIX = "order:token:"
        private const val TOKEN_VALUE = "ACTIVE"

        /**
         * Lua 스크립트: ZPOPMIN + SETEX 원자적 실행
         * 대기열에서 count명을 꺼내면서 동시에 토큰을 발급한다.
         *
         * KEYS[1] = order:queue
         * ARGV[1] = count (꺼낼 수)
         * ARGV[2] = ttlSeconds (토큰 TTL)
         * ARGV[3] = tokenKeyPrefix (order:token:)
         *
         * return: 발급된 유저 수
         */
        private val DEQUEUE_AND_ISSUE_SCRIPT = DefaultRedisScript<Long>(
            """
            local members = redis.call('ZPOPMIN', KEYS[1], tonumber(ARGV[1]))
            local issued = 0
            for i = 1, #members, 2 do
                local userId = members[i]
                local tokenKey = ARGV[3] .. userId
                redis.call('SETEX', tokenKey, tonumber(ARGV[2]), 'ACTIVE')
                issued = issued + 1
            end
            return issued
            """.trimIndent(),
            Long::class.java,
        )

        /**
         * Lua 스크립트: INCR 카운터 + ZADD NX 원자적 실행
         * KEYS[1] = order:queue
         * KEYS[2] = order:queue:counter
         * ARGV[1] = milliseconds (timestamp)
         * ARGV[2] = userId (member)
         *
         * score = ms + counter/1000000 (유니크 보장)
         * return 1: 성공 (추가됨)
         * return 0: 중복 요청 (이미 존재)
         */
        private val ENQUEUE_SCRIPT = DefaultRedisScript<Long>(
            """
            local counter = redis.call('INCR', KEYS[2])
            local score = tonumber(ARGV[1]) + counter / 1000000
            local added = redis.call('ZADD', KEYS[1], 'NX', score, ARGV[2])
            if added == 1 then return 1 else return 0 end
            """.trimIndent(),
            Long::class.java,
        )
    }

    /**
     * 대기열에 유저를 추가한다.
     * @return 1: 성공, 0: 중복
     */
    fun enqueue(userId: Long): Long {
        val ms = System.currentTimeMillis().toString()
        return redisTemplate.execute(
            ENQUEUE_SCRIPT,
            listOf(QUEUE_KEY, COUNTER_KEY),
            ms,
            userId.toString(),
        ) ?: 0L
    }

    /**
     * 대기열에서 유저의 순번을 조회한다. (1-based)
     * @return 순번 (없으면 null)
     */
    fun getPosition(userId: Long): Long? {
        val rank = redisTemplate.opsForZSet().rank(QUEUE_KEY, userId.toString())
        return rank?.plus(1)
    }

    /**
     * 대기열의 전체 크기를 조회한다.
     */
    fun getTotalSize(): Long {
        return redisTemplate.opsForZSet().size(QUEUE_KEY) ?: 0L
    }

    /**
     * 대기열에서 score가 가장 낮은 유저부터 count명을 꺼낸다.
     * @return 꺼낸 userId 목록 (순서 보장)
     */
    fun dequeue(count: Long): List<Long> {
        return redisTemplate.opsForZSet()
            .popMin(QUEUE_KEY, count)
            ?.mapNotNull { it.value?.toLongOrNull() }
            ?: emptyList()
    }

    /**
     * 입장 토큰을 발급한다.
     */
    fun issueToken(userId: Long, ttlSeconds: Long) {
        redisTemplate.opsForValue().set(
            tokenKey(userId),
            TOKEN_VALUE,
            ttlSeconds,
            TimeUnit.SECONDS,
        )
    }

    /**
     * 입장 토큰 보유 여부를 확인한다.
     */
    fun hasToken(userId: Long): Boolean {
        return redisTemplate.hasKey(tokenKey(userId))
    }

    /**
     * 입장 토큰을 소비(삭제)한다.
     * @return true: 소비 성공, false: 토큰 없음
     */
    fun consumeToken(userId: Long): Boolean {
        return redisTemplate.delete(tokenKey(userId))
    }

    /**
     * 토큰의 남은 TTL을 조회한다.
     * @return TTL(초). 키가 없으면 -2, TTL이 없으면 -1
     */
    fun getTokenTtl(userId: Long): Long {
        return redisTemplate.getExpire(tokenKey(userId), TimeUnit.SECONDS)
    }

    /**
     * 대기열에서 count명을 꺼내면서 동시에 토큰을 발급한다. (원자적 실행)
     * @return 발급된 유저 수
     */
    fun dequeueAndIssueTokens(count: Long, ttlSeconds: Long): Long {
        return redisTemplate.execute(
            DEQUEUE_AND_ISSUE_SCRIPT,
            listOf(QUEUE_KEY),
            count.toString(),
            ttlSeconds.toString(),
            TOKEN_KEY_PREFIX,
        ) ?: 0L
    }

    private fun tokenKey(userId: Long): String = "$TOKEN_KEY_PREFIX$userId"
}
