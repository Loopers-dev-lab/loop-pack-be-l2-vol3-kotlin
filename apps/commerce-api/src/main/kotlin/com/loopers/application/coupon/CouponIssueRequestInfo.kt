package com.loopers.application.coupon

import com.loopers.domain.coupon.CouponIssueRequest
import java.time.ZonedDateTime

data class CouponIssueRequestInfo(
    val requestId: String,
    val couponId: Long,
    val userId: Long,
    val status: String,
    val rejectReason: String?,
    val createdAt: ZonedDateTime,
    val processedAt: ZonedDateTime?,
) {
    companion object {
        fun from(request: CouponIssueRequest): CouponIssueRequestInfo {
            return CouponIssueRequestInfo(
                requestId = request.requestId,
                couponId = request.couponId,
                userId = request.userId,
                status = request.status.name,
                rejectReason = request.rejectReason,
                createdAt = request.createdAt,
                processedAt = request.processedAt,
            )
        }
    }
}
