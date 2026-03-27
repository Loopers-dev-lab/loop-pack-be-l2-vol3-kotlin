package com.loopers.domain.coupon

interface IssuedCouponRepository {
    fun save(issuedCoupon: IssuedCoupon): IssuedCoupon

    fun findById(id: Long): IssuedCoupon?

    fun findByCouponIdAndUserId(couponId: Long, userId: Long): IssuedCoupon?
}
