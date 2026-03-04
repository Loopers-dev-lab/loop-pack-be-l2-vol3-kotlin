package com.loopers.application.coupon

import com.loopers.domain.coupon.Coupon
import com.loopers.domain.coupon.DiscountType
import java.time.ZonedDateTime

data class CouponInfo(
    val id: Long,
    val name: String,
    val discountType: DiscountType,
    val discountValue: Long,
    val totalQuantity: Int,
    val issuedQuantity: Int,
    val expiresAt: ZonedDateTime,
    val createdAt: ZonedDateTime,
) {
    companion object {
        fun from(coupon: Coupon): CouponInfo {
            return CouponInfo(
                id = coupon.id,
                name = coupon.name,
                discountType = coupon.discount.type,
                discountValue = coupon.discount.value,
                totalQuantity = coupon.quantity.total,
                issuedQuantity = coupon.quantity.issued,
                expiresAt = coupon.expiresAt,
                createdAt = coupon.createdAt,
            )
        }
    }
}
