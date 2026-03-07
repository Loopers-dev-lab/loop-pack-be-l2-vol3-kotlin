package com.loopers.application.coupon

import com.loopers.domain.coupon.Coupon
import com.loopers.domain.coupon.CouponType
import java.math.BigDecimal
import java.time.ZonedDateTime

data class CouponInfo(
    val id: Long,
    val name: String,
    val type: CouponType,
    val value: BigDecimal,
    val minOrderAmount: BigDecimal?,
    val expiredAt: ZonedDateTime,
    val createdAt: ZonedDateTime,
) {
    companion object {
        fun from(coupon: Coupon): CouponInfo {
            return CouponInfo(
                id = coupon.id,
                name = coupon.name,
                type = coupon.type,
                value = coupon.value,
                minOrderAmount = coupon.minOrderAmount,
                expiredAt = coupon.expiredAt,
                createdAt = coupon.createdAt,
            )
        }
    }
}
