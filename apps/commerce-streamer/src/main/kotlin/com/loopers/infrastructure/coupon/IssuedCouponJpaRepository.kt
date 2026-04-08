package com.loopers.infrastructure.coupon

import com.loopers.domain.coupon.IssuedCoupon
import org.springframework.data.jpa.repository.JpaRepository

interface IssuedCouponJpaRepository : JpaRepository<IssuedCoupon, Long> {
    fun existsByUserIdAndCouponTemplateId(userId: Long, couponTemplateId: Long): Boolean
}
