package com.loopers.domain.coupon

interface IssuedCouponRepository {
    fun save(issuedCoupon: IssuedCoupon): IssuedCoupon
    fun existsByCouponIdAndUserId(couponId: Long, userId: Long): Boolean
    fun findByCouponIdAndUserId(couponId: Long, userId: Long): IssuedCoupon?
    fun findByUserId(userId: Long): List<IssuedCoupon>
    fun findByCouponId(couponId: Long): List<IssuedCoupon>
    fun findByCouponIdAndUserIdWithLock(couponId: Long, userId: Long): IssuedCoupon?
}
