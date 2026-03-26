package com.loopers.infrastructure.coupon

import com.loopers.config.redis.RedisConfig.Companion.REDIS_TEMPLATE_MASTER
import com.loopers.domain.coupon.CouponIssueRepository
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.io.ClassPathResource
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.script.RedisScript
import org.springframework.stereotype.Component

@Component
class CouponIssueRepositoryImpl(
    @Qualifier(REDIS_TEMPLATE_MASTER)
    private val masterRedisTemplate: RedisTemplate<String, String>,
) : CouponIssueRepository {

    companion object {
        private const val ISSUED_SET_KEY_PREFIX = "coupon-issued"
        private const val MAX_QUANTITY_KEY_PREFIX = "coupon-max"
    }

    private val issueScript = RedisScript.of<Long>(
        ClassPathResource("scripts/coupon_issue.lua"),
        Long::class.java,
    )

    override fun tryIssue(couponId: Long, userId: Long, maxQuantity: Int): Long {
        return masterRedisTemplate.execute(
            issueScript,
            listOf("$ISSUED_SET_KEY_PREFIX:$couponId"),
            maxQuantity.toString(),
            userId.toString(),
        ) ?: -1
    }

    override fun restore(couponId: Long, userId: Long) {
        masterRedisTemplate.opsForSet()
            .remove("$ISSUED_SET_KEY_PREFIX:$couponId", userId.toString())
    }

    override fun initCouponStock(couponId: Long, maxQuantity: Int) {
        masterRedisTemplate.opsForValue()
            .set("$MAX_QUANTITY_KEY_PREFIX:$couponId", maxQuantity.toString())
    }
}
