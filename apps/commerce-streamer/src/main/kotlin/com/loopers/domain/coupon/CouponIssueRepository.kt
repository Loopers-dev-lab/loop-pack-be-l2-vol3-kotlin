package com.loopers.domain.coupon

import java.time.ZonedDateTime

interface CouponIssueRepository {
    fun findCouponById(id: Long): CouponInfo?
    fun incrementIssuedCount(couponId: Long): Int
    fun existsUserCoupon(couponId: Long, userId: Long): Boolean
    fun saveUserCoupon(
        couponId: Long,
        userId: Long,
        discountType: String,
        discountValue: Long,
        minOrderAmount: Long,
        expiredAt: ZonedDateTime,
    )
}
