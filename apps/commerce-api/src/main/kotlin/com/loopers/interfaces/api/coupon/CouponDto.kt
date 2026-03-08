package com.loopers.interfaces.api.coupon

import com.loopers.application.coupon.MyCouponInfo
import com.loopers.domain.coupon.IssuedCouponStatus
import java.time.ZonedDateTime

class CouponDto {

    data class MyCouponResponse(
        val couponId: Long,
        val name: String,
        val discountType: String,
        val discountValue: Long,
        val status: IssuedCouponStatus,
        val expiresAt: ZonedDateTime,
        val issuedAt: ZonedDateTime,
    ) {
        companion object {
            fun from(info: MyCouponInfo): MyCouponResponse {
                return MyCouponResponse(
                    couponId = info.couponId,
                    name = info.name,
                    discountType = info.discountType.name,
                    discountValue = info.discountValue,
                    status = info.status,
                    expiresAt = info.expiresAt,
                    issuedAt = info.issuedAt,
                )
            }
        }
    }
}
