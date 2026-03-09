package com.loopers.application.coupon

import com.loopers.domain.coupon.IssuedCoupon
import com.loopers.domain.coupon.IssuedCouponStatus
import java.time.ZonedDateTime

data class IssuedCouponInfo(
    val id: Long,
    val couponId: Long,
    val userId: Long,
    val status: IssuedCouponStatus,
    val usedAt: ZonedDateTime?,
    val createdAt: ZonedDateTime,
) {
    companion object {
        fun from(issuedCoupon: IssuedCoupon): IssuedCouponInfo {
            return IssuedCouponInfo(
                id = issuedCoupon.id,
                couponId = issuedCoupon.couponId,
                userId = issuedCoupon.userId,
                status = issuedCoupon.status,
                usedAt = issuedCoupon.usedAt,
                createdAt = issuedCoupon.createdAt,
            )
        }
    }
}
