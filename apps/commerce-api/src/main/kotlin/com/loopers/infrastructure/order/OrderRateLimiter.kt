package com.loopers.infrastructure.order

import com.loopers.config.redis.RedisConfig
import com.loopers.infrastructure.orderqueue.OrderQueueProperties
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.script.DefaultRedisScript
import org.springframework.stereotype.Component

@Component
class OrderRateLimiter(
    @Qualifier(RedisConfig.REDIS_TEMPLATE_MASTER)
    private val redisTemplate: RedisTemplate<String, String>,
    private val orderQueueProperties: OrderQueueProperties,
) {
    companion object {
        private const val KEY = "order:rate-limit"

        /**
         * 슬라이딩 윈도우 Rate Limiter (Lua 스크립트)
         *
         * 1. 현재 윈도우 밖의 오래된 요청을 제거 (ZREMRANGEBYSCORE)
         * 2. 현재 윈도우 내 요청 수를 조회 (ZCARD)
         * 3. 제한 이내이면 현재 요청을 추가 (ZADD) + TTL 설정
         *
         * KEYS[1] = order:rate-limit
         * ARGV[1] = 현재 시간 (밀리초)
         * ARGV[2] = 윈도우 시작 시간 (현재 - 1000ms)
         * ARGV[3] = 최대 허용 요청 수
         * ARGV[4] = TTL (초)
         *
         * return 1: 허용, 0: 거부
         */
        private val RATE_LIMIT_SCRIPT = DefaultRedisScript<Long>(
            """
            redis.call('ZREMRANGEBYSCORE', KEYS[1], '0', ARGV[2])
            local count = redis.call('ZCARD', KEYS[1])
            if count < tonumber(ARGV[3]) then
                redis.call('ZADD', KEYS[1], ARGV[1], ARGV[1] .. ':' .. count)
                redis.call('EXPIRE', KEYS[1], ARGV[4])
                return 1
            else
                return 0
            end
            """.trimIndent(),
            Long::class.java,
        )
    }

    fun checkRate() {
        val rateLimit = orderQueueProperties.rateLimit
        val now = System.currentTimeMillis()
        val windowStart = now - (rateLimit.windowSeconds * 1000)

        val allowed = redisTemplate.execute(
            RATE_LIMIT_SCRIPT,
            listOf(KEY),
            now.toString(),
            windowStart.toString(),
            rateLimit.maxRequests.toString(),
            (rateLimit.windowSeconds + 1).toString(),
        ) ?: 0L

        if (allowed == 0L) {
            throw CoreException(ErrorType.TOO_MANY_REQUESTS, "주문 요청이 너무 많습니다. 잠시 후 다시 시도해주세요.")
        }
    }
}
