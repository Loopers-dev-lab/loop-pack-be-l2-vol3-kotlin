package com.loopers.interfaces.api.coupon.dto

import com.loopers.application.coupon.MyCouponInfo
import java.math.BigDecimal

class CouponV1Dto {

    data class MyCouponResponse(
        val id: Long,
        val couponName: String,
        val couponType: String,
        val couponValue: Long,
        val maxDiscount: BigDecimal?,
        val status: String,
        val usedAt: String?,
        val createdAt: String,
        val expiredAt: String,
    ) {
        companion object {
            fun from(info: MyCouponInfo): MyCouponResponse {
                return MyCouponResponse(
                    id = info.id,
                    couponName = info.couponName,
                    couponType = info.couponType,
                    couponValue = info.couponValue,
                    maxDiscount = info.maxDiscount,
                    status = info.status,
                    usedAt = info.usedAt,
                    createdAt = info.createdAt,
                    expiredAt = info.expiredAt,
                )
            }
        }
    }
}
