package com.loopers.interfaces.api.v1.coupon

import com.loopers.application.coupon.UserCouponInfo
import java.time.ZonedDateTime

data class GetMyCouponResponse(
    val id: Long,
    val couponId: Long,
    val status: String,
    val discountType: String,
    val discountValue: Long,
    val minOrderAmount: Long,
    val expiredAt: ZonedDateTime,
    val usedAt: ZonedDateTime?,
    val issuedAt: ZonedDateTime,
) {
    companion object {
        fun from(info: UserCouponInfo) = GetMyCouponResponse(
            id = info.id,
            couponId = info.couponId,
            status = info.status,
            discountType = info.discountType,
            discountValue = info.discountValue,
            minOrderAmount = info.minOrderAmount,
            expiredAt = info.expiredAt,
            usedAt = info.usedAt,
            issuedAt = info.issuedAt,
        )
    }
}
