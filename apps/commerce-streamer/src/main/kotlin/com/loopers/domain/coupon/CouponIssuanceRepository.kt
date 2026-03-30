package com.loopers.domain.coupon

interface CouponIssuanceRepository {
    fun findCouponById(couponId: Long): CouponInfo?
    fun save(couponIssuance: CouponIssuance): CouponIssuance
    fun existsByCouponIdAndUserId(couponId: Long, userId: Long): Boolean
    fun countByCouponId(couponId: Long): Long
}
