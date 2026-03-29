package com.loopers.domain.coupon.repository

interface IssuedCouponRepository {
    fun existsByRefCouponIdAndRefUserId(couponId: Long, userId: Long): Boolean
    fun save(refCouponId: Long, refUserId: Long)
}
