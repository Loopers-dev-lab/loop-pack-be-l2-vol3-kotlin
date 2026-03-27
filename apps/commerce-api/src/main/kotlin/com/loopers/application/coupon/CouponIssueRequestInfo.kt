package com.loopers.application.coupon

import com.loopers.domain.coupon.CouponIssueRequestModel
import com.loopers.domain.coupon.CouponIssueRequestStatus
import java.time.ZonedDateTime

data class CouponIssueRequestInfo(
    val requestId: Long,
    val couponId: Long,
    val userId: Long,
    val status: CouponIssueRequestStatus,
    val couponIssueId: Long?,
    val createdAt: ZonedDateTime?,
    val updatedAt: ZonedDateTime?,
) {
    companion object {
        fun from(request: CouponIssueRequestModel): CouponIssueRequestInfo {
            return CouponIssueRequestInfo(
                requestId = request.id,
                couponId = request.couponId,
                userId = request.userId,
                status = request.status,
                couponIssueId = request.couponIssueId,
                createdAt = runCatching { request.createdAt }.getOrNull(),
                updatedAt = runCatching { request.updatedAt }.getOrNull(),
            )
        }
    }
}
