package com.loopers.infrastructure.coupon

import org.springframework.data.jpa.repository.JpaRepository

interface UserCouponJpaRepository : JpaRepository<UserCouponEntity, Long> {
    fun existsByCouponIdAndUserId(couponId: Long, userId: Long): Boolean
}
