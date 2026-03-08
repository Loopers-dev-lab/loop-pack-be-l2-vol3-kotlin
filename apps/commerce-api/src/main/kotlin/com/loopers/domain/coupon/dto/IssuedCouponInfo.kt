package com.loopers.domain.coupon.dto

import com.loopers.domain.coupon.Coupon
import com.loopers.domain.coupon.CouponStatus
import java.time.ZonedDateTime

data class IssuedCouponInfo(
    val id: Long,
    val userId: Long,
    val templateId: Long,
    val templateName: String,
    val status: CouponStatus,
    val issuedAt: ZonedDateTime,
    val usedAt: ZonedDateTime?,
) {
    companion object {
        fun from(coupon: Coupon, templateName: String): IssuedCouponInfo {
            return IssuedCouponInfo(
                id = coupon.id,
                userId = coupon.userId,
                templateId = coupon.templateId,
                templateName = templateName,
                status = coupon.status,
                issuedAt = coupon.createdAt,
                usedAt = coupon.usedAt,
            )
        }
    }
}
