package com.loopers.application.coupon

import com.loopers.domain.coupon.Coupon
import java.time.ZonedDateTime

data class CouponInfo(
    val id: Long,
    val name: String,
    val type: String,
    val value: Long,
    val minOrderAmount: Long?,
    val expiredAt: ZonedDateTime,
    val createdAt: ZonedDateTime,
) {
    companion object {
        fun from(coupon: Coupon): CouponInfo {
            return CouponInfo(
                id = coupon.id,
                name = coupon.name,
                type = coupon.type.name,
                value = coupon.value,
                minOrderAmount = coupon.minOrderAmount,
                expiredAt = coupon.expiredAt,
                createdAt = coupon.createdAt,
            )
        }
    }
}
