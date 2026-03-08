package com.loopers.domain.coupon.dto

import com.loopers.domain.coupon.Coupon
import com.loopers.domain.coupon.CouponStatus
import com.loopers.domain.coupon.CouponType
import java.math.BigDecimal
import java.time.ZonedDateTime

data class CouponInfo(
    val id: Long,
    val templateName: String,
    val type: CouponType,
    val value: BigDecimal,
    val status: CouponStatus,
    val minOrderAmount: BigDecimal,
    val expiredAt: ZonedDateTime,
    val issuedAt: ZonedDateTime,
    val usedAt: ZonedDateTime?,
) {
    companion object {
        fun from(
            coupon: Coupon,
            templateName: String,
            type: CouponType,
            value: BigDecimal,
            minOrderAmount: BigDecimal,
            expiredAt: ZonedDateTime,
        ): CouponInfo {
            return CouponInfo(
                id = coupon.id,
                templateName = templateName,
                type = type,
                value = value,
                status = coupon.status,
                minOrderAmount = minOrderAmount,
                expiredAt = expiredAt,
                issuedAt = coupon.createdAt,
                usedAt = coupon.usedAt,
            )
        }
    }
}
