package com.loopers.infrastructure.coupon

import com.loopers.domain.coupon.IssuedCoupon
import org.springframework.data.jpa.repository.JpaRepository

interface IssuedCouponJpaRepository : JpaRepository<IssuedCoupon, Long> {
    fun existsByCouponIdAndUserId(couponId: Long, userId: Long): Boolean
    fun findByCouponIdAndUserId(couponId: Long, userId: Long): IssuedCoupon?
    fun findByUserId(userId: Long): List<IssuedCoupon>
    fun findByCouponId(couponId: Long): List<IssuedCoupon>
}
