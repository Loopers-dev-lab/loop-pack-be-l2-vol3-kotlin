package com.loopers.application.coupon

import com.loopers.domain.coupon.model.Coupon
import com.loopers.domain.coupon.model.IssuedCoupon
import java.math.BigDecimal
import java.time.format.DateTimeFormatter

data class CouponInfo(
    val id: Long,
    val name: String,
    val type: String,
    val value: Long,
    val maxDiscount: BigDecimal?,
    val minOrderAmount: BigDecimal?,
    val totalQuantity: Int?,
    val issuedCount: Int,
    val expiredAt: String,
) {
    companion object {
        fun from(coupon: Coupon): CouponInfo = CouponInfo(
            id = coupon.id.value,
            name = coupon.name,
            type = coupon.type.name,
            value = coupon.value,
            maxDiscount = coupon.maxDiscount?.value,
            minOrderAmount = coupon.minOrderAmount?.value,
            totalQuantity = coupon.totalQuantity,
            issuedCount = coupon.issuedCount,
            expiredAt = coupon.expiredAt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
        )
    }
}

data class IssuedCouponInfo(
    val id: Long,
    val couponId: Long,
    val userId: Long,
    val status: String,
    val usedAt: String?,
    val createdAt: String,
) {
    companion object {
        fun from(issuedCoupon: IssuedCoupon): IssuedCouponInfo = IssuedCouponInfo(
            id = issuedCoupon.id,
            couponId = issuedCoupon.refCouponId.value,
            userId = issuedCoupon.refUserId.value,
            status = issuedCoupon.status.name,
            usedAt = issuedCoupon.usedAt?.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
            createdAt = issuedCoupon.createdAt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
        )
    }
}

data class MyCouponInfo(
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
        fun from(issuedCoupon: IssuedCoupon, coupon: Coupon): MyCouponInfo = MyCouponInfo(
            id = issuedCoupon.id,
            couponName = coupon.name,
            couponType = coupon.type.name,
            couponValue = coupon.value,
            maxDiscount = coupon.maxDiscount?.value,
            status = issuedCoupon.status.name,
            usedAt = issuedCoupon.usedAt?.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
            createdAt = issuedCoupon.createdAt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
            expiredAt = coupon.expiredAt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
        )
    }
}
