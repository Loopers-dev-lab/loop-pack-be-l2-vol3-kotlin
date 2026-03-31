package com.loopers.infrastructure.coupon

import com.loopers.domain.coupon.IssuedCouponModel
import org.springframework.data.jpa.repository.JpaRepository

interface IssuedCouponJpaRepository : JpaRepository<IssuedCouponModel, Long> {
    fun findByCouponIdAndUserIdAndDeletedAtIsNull(couponId: Long, userId: Long): IssuedCouponModel?
}
