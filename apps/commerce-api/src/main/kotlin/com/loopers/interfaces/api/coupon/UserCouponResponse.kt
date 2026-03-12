package com.loopers.interfaces.api.coupon

import com.loopers.application.coupon.UserCouponInfo
import java.time.ZonedDateTime

data class UserCouponResponse(
    val id: Long,
    val couponId: Long,
    val status: String,
    val expiredAt: ZonedDateTime,
    val usedAt: ZonedDateTime?,
    val createdAt: ZonedDateTime,
) {
    companion object {
        fun from(info: UserCouponInfo): UserCouponResponse {
            return UserCouponResponse(
                id = info.id,
                couponId = info.couponId,
                status = info.status,
                expiredAt = info.expiredAt,
                usedAt = info.usedAt,
                createdAt = info.createdAt,
            )
        }
    }
}
