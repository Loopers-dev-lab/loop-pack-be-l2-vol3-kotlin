package com.loopers.infrastructure.coupon

import com.loopers.config.redis.RedisConfig
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.script.DefaultRedisScript
import org.springframework.stereotype.Component

@Component
class CouponStockRedisRepository(
    @Qualifier(RedisConfig.REDIS_TEMPLATE_MASTER)
    private val redisTemplate: RedisTemplate<String, String>,
) {
    companion object {
        private const val QUEUE_KEY_PREFIX = "coupon:queue"
        private const val MAX_QTY_KEY_PREFIX = "coupon:max-qty"
        private const val ACTIVE_QUEUES_KEY = "coupon:active-queues"

        /**
         * Lua 스크립트: 수량 체크 + ZADD NX 원자적 실행
         * KEYS[1] = coupon:queue:{couponId}
         * KEYS[2] = coupon:max-qty:{couponId}
         * ARGV[1] = score (timestamp)
         * ARGV[2] = member (userId)
         *
         * return 1: 성공 (추가됨)
         * return 0: 중복 요청 (이미 존재)
         * return -1: 수량 소진
         */
        private val ADD_TO_QUEUE_SCRIPT = DefaultRedisScript<Long>(
            """
            local maxQty = tonumber(redis.call('GET', KEYS[2]))
            if maxQty == nil then return -2 end
            local currentSize = redis.call('ZCARD', KEYS[1])
            if currentSize >= maxQty then return -1 end
            local added = redis.call('ZADD', KEYS[1], 'NX', ARGV[1], ARGV[2])
            if added == 1 then return 1 else return 0 end
            """.trimIndent(),
            Long::class.java,
        )
    }

    fun initialize(couponId: Long, maxQuantity: Int) {
        redisTemplate.opsForValue().set(maxQtyKey(couponId), maxQuantity.toString())
        redisTemplate.opsForSet().add(ACTIVE_QUEUES_KEY, couponId.toString())
    }

    /**
     * @return 1: 성공, 0: 중복 요청, -1: 수량 소진, -2: 초기화 안됨
     */
    fun tryEnqueue(couponId: Long, userId: Long): Long {
        val score = System.currentTimeMillis().toDouble()
        return redisTemplate.execute(
            ADD_TO_QUEUE_SCRIPT,
            listOf(queueKey(couponId), maxQtyKey(couponId)),
            score.toString(),
            userId.toString(),
        ) ?: -2L
    }

    fun dequeue(couponId: Long, count: Long): Set<String> {
        return redisTemplate.opsForZSet()
            .popMin(queueKey(couponId), count)
            ?.map { it.value!! }
            ?.toSet()
            ?: emptySet()
    }

    private fun queueKey(couponId: Long): String = "$QUEUE_KEY_PREFIX:$couponId"
    private fun maxQtyKey(couponId: Long): String = "$MAX_QTY_KEY_PREFIX:$couponId"
}
