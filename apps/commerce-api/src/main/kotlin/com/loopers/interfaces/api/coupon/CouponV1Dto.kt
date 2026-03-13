package com.loopers.interfaces.api.coupon

import com.loopers.application.coupon.IssuedCouponInfo
import com.loopers.domain.coupon.CouponStatus
import com.loopers.domain.coupon.CouponType
import java.time.ZonedDateTime

class CouponV1Dto {
    data class IssuedCouponResponse(
        val id: Long,
        val couponTemplateId: Long,
        val couponName: String,
        val couponType: CouponType,
        val couponValue: Long,
        val minOrderAmount: Long?,
        val status: CouponStatus,
        val expiredAt: ZonedDateTime,
    ) {
        companion object {
            fun from(info: IssuedCouponInfo): IssuedCouponResponse {
                return IssuedCouponResponse(
                    id = info.id,
                    couponTemplateId = info.couponTemplateId,
                    couponName = info.couponName,
                    couponType = info.couponType,
                    couponValue = info.couponValue,
                    minOrderAmount = info.minOrderAmount,
                    status = info.status,
                    expiredAt = info.expiredAt,
                )
            }
        }
    }
}
