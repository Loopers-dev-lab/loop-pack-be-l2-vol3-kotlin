package com.loopers.application.coupon

import com.loopers.domain.coupon.CouponStatus
import com.loopers.domain.coupon.DiscountType
import com.loopers.domain.coupon.IssuedCouponInfo
import java.time.ZonedDateTime

data class UserIssuedCouponResult(
    val id: Long,
    val couponId: Long,
    val discountType: DiscountType,
    val discountValue: Int,
    val status: CouponStatus,
    val expiredAt: ZonedDateTime,
    val usedAt: ZonedDateTime?,
) {
    companion object {
        fun from(info: IssuedCouponInfo): UserIssuedCouponResult {
            return UserIssuedCouponResult(
                id = info.id,
                couponId = info.couponId,
                discountType = info.discountType,
                discountValue = info.discountValue,
                status = info.status,
                expiredAt = info.expiredAt,
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
