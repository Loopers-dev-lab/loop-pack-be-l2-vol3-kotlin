package com.loopers.interfaces.api.coupon

import com.loopers.application.coupon.CouponIssueRequestInfo
import com.loopers.domain.coupon.CouponIssueStatus
import java.time.ZonedDateTime

class CouponIssueAsyncDto {

    data class IssueAsyncResponse(
        val requestId: String,
        val status: CouponIssueStatus,
    )

    data class IssueRequestResponse(
        val requestId: String,
        val couponId: Long,
        val status: CouponIssueStatus,
        val failReason: String?,
        val createdAt: ZonedDateTime,
    ) {
        companion object {
            fun from(info: CouponIssueRequestInfo): IssueRequestResponse {
                return IssueRequestResponse(
                    requestId = info.requestId,
                    couponId = info.couponId,
                    status = info.status,
                    failReason = info.failReason,
                    createdAt = info.createdAt,
                )
            }
        }
    }
}
