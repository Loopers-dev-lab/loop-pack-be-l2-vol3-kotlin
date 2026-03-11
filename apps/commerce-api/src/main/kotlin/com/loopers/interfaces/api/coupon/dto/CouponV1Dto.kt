package com.loopers.interfaces.api.coupon.dto

import com.loopers.application.coupon.IssuedCouponInfo
import com.loopers.application.coupon.MyCouponInfo
import java.math.BigDecimal
import java.time.format.DateTimeFormatter

class CouponV1Dto {

    data class IssueCouponResponse(
        val id: Long,
        val couponId: Long,
        val userId: Long,
        val status: String,
        val usedAt: String?,
        val createdAt: String,
    ) {
        companion object {
            fun from(info: IssuedCouponInfo): IssueCouponResponse {
                return IssueCouponResponse(
                    id = info.id,
                    couponId = info.couponId,
                    userId = info.userId,
                    status = info.status,
                    usedAt = info.usedAt?.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                    createdAt = info.createdAt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                )
            }
        }
    }

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
                    usedAt = info.usedAt?.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                    createdAt = info.createdAt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                    expiredAt = info.expiredAt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                )
            }
        }
    }
}
