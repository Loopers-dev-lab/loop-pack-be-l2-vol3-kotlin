package com.loopers.application.coupon

import com.loopers.domain.coupon.CouponIssueModel
import com.loopers.domain.coupon.CouponIssueStatus
import com.loopers.domain.coupon.CouponModel
import com.loopers.domain.coupon.CouponType
import java.time.ZonedDateTime

data class CouponIssueInfo(
    val id: Long,
    val couponId: Long,
    val couponName: String,
    val couponType: CouponType,
    val couponValue: Long,
    val minOrderAmount: Long?,
    val status: CouponIssueStatus,
    val expiredAt: ZonedDateTime,
    val createdAt: ZonedDateTime?,
) {
    companion object {
        fun of(issue: CouponIssueModel, coupon: CouponModel): CouponIssueInfo {
            return CouponIssueInfo(
                id = issue.id,
                couponId = issue.couponId,
                couponName = coupon.name,
                couponType = coupon.type,
                couponValue = coupon.value,
                minOrderAmount = coupon.minOrderAmount,
                status = if (issue.status == CouponIssueStatus.AVAILABLE && coupon.isExpired()) {
                    CouponIssueStatus.EXPIRED
                } else {
                    issue.status
                },
                expiredAt = coupon.expiredAt,
                createdAt = runCatching { issue.createdAt }.getOrNull(),
            )
        }
    }
}
