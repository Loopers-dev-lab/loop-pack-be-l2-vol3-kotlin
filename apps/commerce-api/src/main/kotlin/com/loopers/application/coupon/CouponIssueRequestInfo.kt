package com.loopers.application.coupon

import com.loopers.domain.coupon.CouponIssueRequest
import com.loopers.domain.coupon.CouponIssueStatus
import java.time.ZonedDateTime

data class CouponIssueRequestInfo(
    val requestId: String,
    val couponId: Long,
    val status: CouponIssueStatus,
    val failReason: String?,
    val createdAt: ZonedDateTime,
) {
    companion object {
        fun from(request: CouponIssueRequest): CouponIssueRequestInfo {
            return CouponIssueRequestInfo(
                requestId = request.requestId,
                couponId = request.couponId,
                status = request.status,
                failReason = request.failReason,
                createdAt = request.createdAt,
            )
        }
    }
}
