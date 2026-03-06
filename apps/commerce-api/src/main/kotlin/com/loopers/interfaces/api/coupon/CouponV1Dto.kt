package com.loopers.interfaces.api.coupon

import com.loopers.application.coupon.IssuedCouponInfo
import java.time.ZonedDateTime

class CouponV1Dto {
    data class IssuedCouponResponse(
        val id: Long,
        val templateName: String,
        val type: String,
        val value: Long,
        val minOrderAmount: Long?,
        val maxDiscountAmount: Long?,
        val status: String,
        val expiredAt: ZonedDateTime,
        val usedAt: ZonedDateTime?,
        val createdAt: ZonedDateTime?,
    ) {
        companion object {
            fun from(info: IssuedCouponInfo): IssuedCouponResponse {
                return IssuedCouponResponse(
                    id = info.id,
                    templateName = info.templateName,
                    type = info.type,
                    value = info.value,
                    minOrderAmount = info.minOrderAmount,
                    maxDiscountAmount = info.maxDiscountAmount,
                    status = info.status,
                    expiredAt = info.expiredAt,
                    usedAt = info.usedAt,
                    createdAt = info.createdAt,
                )
            }
        }
    }
}
