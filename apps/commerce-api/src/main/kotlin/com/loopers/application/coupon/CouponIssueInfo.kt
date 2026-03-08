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
        private const val UNKNOWN_USER_NAME = "알 수 없는 사용자"

        fun from(issuedCoupon: IssuedCoupon, user: User?, couponExpiresAt: ZonedDateTime): CouponIssueInfo {
            return CouponIssueInfo(
                userId = issuedCoupon.userId,
                userName = user?.name ?: UNKNOWN_USER_NAME,
                issuedAt = issuedCoupon.createdAt,
                usedAt = issuedCoupon.usedAt,
                status = issuedCoupon.status(couponExpiresAt),
            )
        }
    }
}
