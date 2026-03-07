package com.loopers.domain.coupon

import com.loopers.domain.Money
import java.time.ZonedDateTime

data class CouponInfo(
    val id: Long,
    val name: String,
    val type: CouponType,
    val value: Long,
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
                expiredAt = coupon.expiredAt,
                createdAt = coupon.createdAt,
            )
        }
    }
}

data class CouponIssueInfo(
    val id: Long,
    val couponId: Long,
    val userId: Long,
    val status: CouponIssueStatus,
    val usedAt: ZonedDateTime?,
    val createdAt: ZonedDateTime,
) {
    companion object {
        fun from(couponIssue: CouponIssue): CouponIssueInfo {
            return CouponIssueInfo(
                id = couponIssue.id,
                couponId = couponIssue.couponId,
                userId = couponIssue.userId,
                status = couponIssue.status,
                usedAt = couponIssue.usedAt,
                createdAt = couponIssue.createdAt,
            )
        }
    }
}

/**
 * 쿠폰 사용 결과. 주문 시 쿠폰 적용 후 Facade에 반환하는 DTO.
 */
data class CouponUsageInfo(
    val couponIssueId: Long,
    val discountAmount: Money,
)
