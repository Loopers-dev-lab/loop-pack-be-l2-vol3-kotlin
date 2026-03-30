package com.loopers.infrastructure.coupon

import com.loopers.domain.coupon.CouponIssuance
import org.springframework.data.jpa.repository.JpaRepository

interface CouponIssuanceJpaRepository : JpaRepository<CouponIssuance, Long> {
    fun existsByCouponIdAndUserId(couponId: Long, userId: Long): Boolean
    fun countByCouponId(couponId: Long): Long
}
