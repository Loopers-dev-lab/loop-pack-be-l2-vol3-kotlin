package com.loopers.application.coupon

import com.loopers.domain.coupon.IssuedCoupon
import com.loopers.domain.coupon.IssuedCouponStatus
import com.loopers.domain.user.User
import java.time.ZonedDateTime

data class CouponIssueInfo(
    val userId: Long,
    val userName: String,
    val issuedAt: ZonedDateTime,
    val usedAt: ZonedDateTime?,
    val status: IssuedCouponStatus,
) {
    companion object {
        fun from(issuedCoupon: IssuedCoupon, user: User, couponExpiresAt: ZonedDateTime): CouponIssueInfo {
            return CouponIssueInfo(
                userId = issuedCoupon.userId,
                userName = user.name,
                issuedAt = issuedCoupon.createdAt,
                usedAt = issuedCoupon.usedAt,
                status = issuedCoupon.status(couponExpiresAt),
            )
        }
    }
}
