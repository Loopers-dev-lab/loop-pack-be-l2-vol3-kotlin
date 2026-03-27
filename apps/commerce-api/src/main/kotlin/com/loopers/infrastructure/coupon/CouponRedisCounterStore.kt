package com.loopers.infrastructure.coupon

import com.loopers.domain.coupon.CouponCounterStore
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component

@Component
class CouponRedisCounterStore(
    private val redisTemplate: RedisTemplate<String, String>,
) : CouponCounterStore {

    override fun increment(couponId: Long): Long {
        val key = counterKey(couponId)
        return redisTemplate.opsForValue().increment(key) ?: 1L
    }

    override fun decrement(couponId: Long) {
        val key = counterKey(couponId)
        redisTemplate.opsForValue().decrement(key)
    }

    companion object {
        private fun counterKey(couponId: Long): String = "coupon:$couponId:counter"
    }
}
