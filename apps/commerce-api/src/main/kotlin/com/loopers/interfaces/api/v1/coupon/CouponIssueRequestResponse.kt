package com.loopers.interfaces.api.v1.coupon

import com.loopers.domain.coupon.CouponIssueRequest

data class CouponIssueRequestResponse(
    val requestId: String,
    val couponId: Long,
    val userId: Long,
    val status: String,
    val failureReason: String?,
) {
    companion object {
        fun from(request: CouponIssueRequest): CouponIssueRequestResponse {
            return CouponIssueRequestResponse(
                requestId = request.requestId,
                couponId = request.refCouponId,
                userId = request.refUserId,
                status = request.status.name,
                failureReason = request.failureReason,
            )
        }
    }
}
