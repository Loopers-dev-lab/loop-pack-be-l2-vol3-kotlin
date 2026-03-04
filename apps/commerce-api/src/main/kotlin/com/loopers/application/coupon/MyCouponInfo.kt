package com.loopers.application.coupon

import com.loopers.domain.coupon.Coupon
import com.loopers.domain.coupon.DiscountType
import com.loopers.domain.coupon.IssuedCoupon
import com.loopers.domain.coupon.IssuedCouponStatus
import java.time.ZonedDateTime

data class MyCouponInfo(
    val couponId: Long,
    val name: String,
    val discountType: DiscountType,
    val discountValue: Long,
    val status: IssuedCouponStatus,
    val expiresAt: ZonedDateTime,
    val issuedAt: ZonedDateTime,
) {
    companion object {
        fun from(issuedCoupon: IssuedCoupon, coupon: Coupon): MyCouponInfo {
            return MyCouponInfo(
                couponId = coupon.id,
                name = coupon.name,
                discountType = coupon.discount.type,
                discountValue = coupon.discount.value,
                status = issuedCoupon.status(coupon.expiresAt),
                expiresAt = coupon.expiresAt,
                issuedAt = issuedCoupon.createdAt,
            )
        }
    }
}
