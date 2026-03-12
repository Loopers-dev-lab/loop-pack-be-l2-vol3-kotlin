package com.loopers.application.coupon

import com.loopers.domain.coupon.UserCoupon
import java.time.ZonedDateTime

data class UserCouponInfo(
    val id: Long,
    val couponId: Long,
    val userId: Long,
    val status: String,
    val expiredAt: ZonedDateTime,
    val usedAt: ZonedDateTime?,
    val usedOrderId: Long?,
    val createdAt: ZonedDateTime,
) {
    companion object {
        fun from(userCoupon: UserCoupon): UserCouponInfo {
            return UserCouponInfo(
                id = userCoupon.id,
                couponId = userCoupon.couponId,
                userId = userCoupon.userId,
                status = userCoupon.status.name,
                expiredAt = userCoupon.expiredAt,
                usedAt = userCoupon.usedAt,
                usedOrderId = userCoupon.usedOrderId,
                createdAt = userCoupon.createdAt,
            )
        }
    }
}
