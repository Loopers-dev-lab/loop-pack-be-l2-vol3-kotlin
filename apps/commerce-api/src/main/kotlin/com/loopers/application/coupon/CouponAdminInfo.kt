package com.loopers.application.coupon

import com.loopers.domain.coupon.CouponIssueModel
import com.loopers.domain.coupon.CouponIssueStatus
import com.loopers.domain.coupon.CouponModel
import com.loopers.domain.coupon.CouponType
import java.time.ZonedDateTime

data class CouponAdminInfo(
    val id: Long,
    val name: String,
    val type: CouponType,
    val value: Long,
    val minOrderAmount: Long?,
    val expiredAt: ZonedDateTime,
    val createdAt: ZonedDateTime?,
    val updatedAt: ZonedDateTime?,
) {
    companion object {
        fun from(coupon: CouponModel): CouponAdminInfo {
            return CouponAdminInfo(
                id = coupon.id,
                name = coupon.name,
                type = coupon.type,
                value = coupon.value,
                minOrderAmount = coupon.minOrderAmount,
                expiredAt = coupon.expiredAt,
                createdAt = runCatching { coupon.createdAt }.getOrNull(),
                updatedAt = runCatching { coupon.updatedAt }.getOrNull(),
            )
        }
    }
}

data class CouponIssueAdminInfo(
    val id: Long,
    val couponId: Long,
    val userId: Long,
    val status: CouponIssueStatus,
    val createdAt: ZonedDateTime?,
) {
    companion object {
        fun from(issue: CouponIssueModel): CouponIssueAdminInfo {
            return CouponIssueAdminInfo(
                id = issue.id,
                couponId = issue.couponId,
                userId = issue.userId,
                status = issue.status,
                createdAt = runCatching { issue.createdAt }.getOrNull(),
            )
        }
    }
}
