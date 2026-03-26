package com.loopers.infrastructure.coupon

import com.loopers.config.redis.RedisConfig.Companion.REDIS_TEMPLATE_MASTER
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component

@Component
class CouponIssueRedisRepository(
    @Qualifier(REDIS_TEMPLATE_MASTER)
    private val masterRedisTemplate: RedisTemplate<String, String>,
) {

    companion object {
        private const val ISSUED_SET_KEY_PREFIX = "coupon-issued"
    }

    fun restore(couponId: Long, userId: Long) {
        masterRedisTemplate.opsForSet()
            .remove("$ISSUED_SET_KEY_PREFIX:$couponId", userId.toString())
    }
}
