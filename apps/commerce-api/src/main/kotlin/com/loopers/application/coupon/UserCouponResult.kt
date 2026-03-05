package com.loopers.application.coupon

import com.loopers.domain.coupon.CouponStatus
import com.loopers.domain.coupon.IssuedCouponInfo
import java.time.ZonedDateTime

data class UserIssuedCouponResult(
    val id: Long,
    val couponId: Long,
    val status: CouponStatus,
    val usedAt: ZonedDateTime?,
) {
    companion object {
        fun from(info: IssuedCouponInfo): UserIssuedCouponResult {
            return UserIssuedCouponResult(
                id = info.id,
                couponId = info.couponId,
                status = info.status,
                usedAt = info.usedAt,
            )
        }
    }
}

data class UserListCouponsResult(
    val content: List<UserIssuedCouponResult>,
    val page: Int,
    val size: Int,
    val hasNext: Boolean,
)
