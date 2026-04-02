package com.loopers.interfaces.api.coupon

import com.loopers.application.coupon.CouponIssueRequestInfo
import java.time.ZonedDateTime

data class CouponIssueRequestResponse(
    val requestId: String,
    val couponId: Long,
    val status: String,
    val rejectReason: String?,
    val createdAt: ZonedDateTime,
    val processedAt: ZonedDateTime?,
) {
    companion object {
        fun from(info: CouponIssueRequestInfo): CouponIssueRequestResponse {
            return CouponIssueRequestResponse(
                requestId = info.requestId,
                couponId = info.couponId,
                status = info.status,
                rejectReason = info.rejectReason,
                createdAt = info.createdAt,
                processedAt = info.processedAt,
            )
        }
    }
}
