package com.loopers.interfaces.api.admin.coupon

import com.loopers.application.coupon.CouponTemplateInfo
import com.loopers.application.coupon.IssuedCouponInfo
import com.loopers.domain.coupon.CouponStatus
import com.loopers.domain.coupon.CouponType
import java.time.ZonedDateTime

class AdminCouponV1Dto {
    data class CreateCouponTemplateRequest(
        val name: String,
        val type: CouponType,
        val value: Long,
        val minOrderAmount: Long?,
        val expiredAt: ZonedDateTime,
    )

    data class UpdateCouponTemplateRequest(
        val name: String,
        val value: Long,
        val minOrderAmount: Long?,
        val expiredAt: ZonedDateTime,
    )

    data class CouponTemplateResponse(
        val id: Long,
        val name: String,
        val type: CouponType,
        val value: Long,
        val minOrderAmount: Long?,
        val expiredAt: ZonedDateTime,
    ) {
        companion object {
            fun from(info: CouponTemplateInfo): CouponTemplateResponse {
                return CouponTemplateResponse(
                    id = info.id,
                    name = info.name,
                    type = info.type,
                    value = info.value,
                    minOrderAmount = info.minOrderAmount,
                    expiredAt = info.expiredAt,
                )
            }
        }
    }

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
