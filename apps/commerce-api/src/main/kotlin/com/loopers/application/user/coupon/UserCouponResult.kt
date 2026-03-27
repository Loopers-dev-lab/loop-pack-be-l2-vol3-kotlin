package com.loopers.application.user.coupon

import com.loopers.domain.coupon.CouponIssueRequest
import com.loopers.domain.coupon.IssuedCoupon
import java.time.ZonedDateTime

class UserCouponResult {
    data class IssueRequest(
        val requestId: Long,
        val couponId: Long,
        val status: String,
    ) {
        companion object {
            fun from(request: CouponIssueRequest): IssueRequest = IssueRequest(
                requestId = request.id!!,
                couponId = request.couponId,
                status = request.status.name,
            )
        }
    }

    data class IssueRequestStatus(
        val requestId: Long,
        val couponId: Long,
        val status: String,
        val failureReasonCode: String?,
        val issuedCouponId: Long?,
    ) {
        companion object {
            fun from(request: CouponIssueRequest): IssueRequestStatus = IssueRequestStatus(
                requestId = request.id!!,
                couponId = request.couponId,
                status = request.status.name,
                failureReasonCode = request.failureReasonCode,
                issuedCouponId = request.issuedCouponId,
            )
        }
    }

    data class Issued(
        val id: Long,
        val couponId: Long,
        val status: String,
        val expiredAt: ZonedDateTime,
    ) {
        companion object {
            fun from(issuedCoupon: IssuedCoupon): Issued = Issued(
                id = issuedCoupon.id!!,
                couponId = issuedCoupon.couponId,
                status = issuedCoupon.status.name,
                expiredAt = issuedCoupon.expiredAt,
            )
        }
    }

    data class ListItem(
        val id: Long,
        val couponId: Long,
        val couponName: String,
        val couponType: String,
        val discountValue: Long,
        val displayStatus: String,
        val expiredAt: ZonedDateTime,
        val usedAt: ZonedDateTime?,
    )
}
