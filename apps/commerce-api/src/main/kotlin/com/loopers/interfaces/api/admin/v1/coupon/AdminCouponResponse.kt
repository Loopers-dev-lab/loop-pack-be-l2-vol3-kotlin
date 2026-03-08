package com.loopers.interfaces.api.admin.v1.coupon

import com.loopers.application.coupon.CouponInfo
import java.time.ZonedDateTime

data class AdminCouponResponse(
    val id: Long,
    val name: String,
    val discountType: String,
    val discountValue: Long,
    val minOrderAmount: Long,
    val maxIssueCount: Int?,
    val issuedCount: Int,
    val expiredAt: ZonedDateTime,
    val deletedAt: ZonedDateTime?,
) {
    companion object {
        fun from(couponInfo: CouponInfo) = AdminCouponResponse(
            id = couponInfo.id,
            name = couponInfo.name,
            discountType = couponInfo.discountType,
            discountValue = couponInfo.discountValue,
            minOrderAmount = couponInfo.minOrderAmount,
            maxIssueCount = couponInfo.maxIssueCount,
            issuedCount = couponInfo.issuedCount,
            expiredAt = couponInfo.expiredAt,
            deletedAt = couponInfo.deletedAt,
        )
    }
}
