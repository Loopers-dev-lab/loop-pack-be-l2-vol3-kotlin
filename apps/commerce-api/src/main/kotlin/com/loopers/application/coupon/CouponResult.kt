package com.loopers.application.coupon

import com.loopers.domain.coupon.CouponInfo
import com.loopers.domain.coupon.CouponIssueInfo
import com.loopers.domain.coupon.CouponIssueStatus
import java.time.ZonedDateTime

/**
 * Admin용 쿠폰 템플릿 결과
 */
data class CouponResult(
    val id: Long,
    val name: String,
    val type: String,
    val value: Long,
    val expiredAt: ZonedDateTime,
    val createdAt: ZonedDateTime,
) {
    companion object {
        fun from(info: CouponInfo): CouponResult {
            return CouponResult(
                id = info.id,
                name = info.name,
                type = info.type.name,
                value = info.value,
                expiredAt = info.expiredAt,
                createdAt = info.createdAt,
            )
        }
    }
}

/**
 * Admin용 쿠폰 발급 내역 결과
 */
data class CouponIssueResult(
    val id: Long,
    val couponId: Long,
    val userId: Long,
    val status: String,
    val usedAt: ZonedDateTime?,
    val createdAt: ZonedDateTime,
) {
    companion object {
        fun from(info: CouponIssueInfo): CouponIssueResult {
            return CouponIssueResult(
                id = info.id,
                couponId = info.couponId,
                userId = info.userId,
                status = info.status.name,
                usedAt = info.usedAt,
                createdAt = info.createdAt,
            )
        }
    }
}

/**
 * User용 내 쿠폰 결과 — Coupon 정보 + CouponIssue 상태를 조합
 *
 * status 판단:
 * - DB status가 USED → USED (확정)
 * - DB status가 AVAILABLE이지만 coupon.expiredAt이 지남 → EXPIRED (시간 기반 판단)
 * - 그 외 → AVAILABLE
 */
data class MyCouponResult(
    val couponIssueId: Long,
    val couponId: Long,
    val name: String,
    val type: String,
    val value: Long,
    val status: String,
    val expiredAt: ZonedDateTime,
    val usedAt: ZonedDateTime?,
    val issuedAt: ZonedDateTime,
) {
    companion object {
        fun of(info: CouponIssueInfo, couponInfo: CouponInfo): MyCouponResult {
            val resolvedStatus = resolveStatus(info.status, couponInfo.expiredAt)
            return MyCouponResult(
                couponIssueId = info.id,
                couponId = info.couponId,
                name = couponInfo.name,
                type = couponInfo.type.name,
                value = couponInfo.value,
                status = resolvedStatus.name,
                expiredAt = couponInfo.expiredAt,
                usedAt = info.usedAt,
                issuedAt = info.createdAt,
            )
        }

        /**
         * 만료 여부를 시간 비교로 판단 (스케줄러 의존 X)
         * - USED는 그대로 유지
         * - AVAILABLE인데 만료 시간이 지났으면 EXPIRED로 보정
         */
        private fun resolveStatus(
            dbStatus: CouponIssueStatus,
            expiredAt: ZonedDateTime,
        ): CouponIssueStatus {
            if (dbStatus == CouponIssueStatus.USED) return CouponIssueStatus.USED
            if (ZonedDateTime.now().isAfter(expiredAt)) return CouponIssueStatus.EXPIRED
            return CouponIssueStatus.AVAILABLE
        }
    }
}
