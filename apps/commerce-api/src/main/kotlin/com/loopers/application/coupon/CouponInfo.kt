package com.loopers.application.coupon

import com.loopers.domain.coupon.model.Coupon
import com.loopers.domain.coupon.model.CouponIssueRequest
import com.loopers.domain.coupon.model.IssuedCoupon
import java.math.BigDecimal
import java.time.ZonedDateTime

data class CouponInfo(
    val id: Long,
    val name: String,
    val type: String,
    val value: Long,
    val maxDiscount: BigDecimal?,
    val minOrderAmount: BigDecimal?,
    val totalQuantity: Int?,
    val issuedCount: Int,
    val expiredAt: ZonedDateTime,
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
            expiredAt = coupon.expiredAt,
        )
    }
}

data class IssuedCouponInfo(
    val id: Long,
    val couponId: Long,
    val userId: Long,
    val status: String,
    val usedAt: ZonedDateTime?,
    val createdAt: ZonedDateTime,
) {
    companion object {
        fun from(issuedCoupon: IssuedCoupon): IssuedCouponInfo = IssuedCouponInfo(
            id = issuedCoupon.id,
            couponId = issuedCoupon.refCouponId.value,
            userId = issuedCoupon.refUserId.value,
            status = issuedCoupon.status.name,
            usedAt = issuedCoupon.usedAt,
            createdAt = issuedCoupon.createdAt,
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
    val usedAt: ZonedDateTime?,
    val createdAt: ZonedDateTime,
    val expiredAt: ZonedDateTime,
) {
    companion object {
        fun from(issuedCoupon: IssuedCoupon, coupon: Coupon): MyCouponInfo = MyCouponInfo(
            id = issuedCoupon.id,
            couponName = coupon.name,
            couponType = coupon.type.name,
            couponValue = coupon.value,
            maxDiscount = coupon.maxDiscount?.value,
            status = issuedCoupon.status.name,
            usedAt = issuedCoupon.usedAt,
            createdAt = issuedCoupon.createdAt,
            expiredAt = coupon.expiredAt,
        )
    }
}

data class CouponIssueRequestInfo(
    val requestId: String,
)

data class CouponIssueStatusInfo(
    val requestId: String,
    val status: String,
) {
    companion object {
        fun from(request: CouponIssueRequest): CouponIssueStatusInfo = CouponIssueStatusInfo(
            requestId = request.requestId,
            status = request.status.name,
        )
    }
}
