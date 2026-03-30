package com.loopers.infrastructure.order

import com.loopers.config.redis.RedisConfig.Companion.REDIS_TEMPLATE_MASTER
import com.loopers.config.redis.RedisKeys
import com.loopers.domain.order.CouponReservationRepository
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class CouponReservationRedisRepository(
    @Qualifier(REDIS_TEMPLATE_MASTER)
    private val masterRedisTemplate: RedisTemplate<String, String>,
) : CouponReservationRepository {

    companion object {
        private val RESERVATION_TTL = Duration.ofMinutes(10)
    }

    override fun reserve(couponId: Long, userId: Long): Boolean {
        return masterRedisTemplate.opsForValue()
            .setIfAbsent(RedisKeys.couponUseKey(couponId, userId), "reserved", RESERVATION_TTL)
            ?: false
    }

    override fun restore(couponId: Long, userId: Long) {
        masterRedisTemplate.delete(RedisKeys.couponUseKey(couponId, userId))
    }
}
