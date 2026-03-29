package com.loopers.infrastructure.coupon

import org.springframework.data.jpa.repository.JpaRepository

interface IssuedCouponJpaRepository : JpaRepository<IssuedCouponEntity, Long> {
    fun existsByCouponIdAndMemberId(couponId: Long, memberId: Long): Boolean
}
