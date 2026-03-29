package com.loopers.domain.coupon

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import java.time.ZonedDateTime

class CouponIssueRequest(
    val id: Long? = null,
    val couponId: Long,
    val memberId: Long,
    status: CouponIssueRequestStatus = CouponIssueRequestStatus.PENDING,
    val requestedAt: ZonedDateTime = ZonedDateTime.now(),
    issuedCouponId: Long? = null,
    failureReason: String? = null,
) {
    var status: CouponIssueRequestStatus = status
        private set

    var issuedCouponId: Long? = issuedCouponId
        private set

    var failureReason: String? = failureReason
        private set

    fun markSucceeded(issuedCouponId: Long) {
        status = CouponIssueRequestStatus.SUCCEEDED
        this.issuedCouponId = issuedCouponId
        failureReason = null
    }

    fun markFailed(status: CouponIssueRequestStatus, reason: String) {
        this.status = status
        failureReason = reason
    }

    fun validateOwner(memberId: Long) {
        if (this.memberId != memberId) {
            throw CoreException(ErrorType.COUPON_ISSUE_REQUEST_NOT_OWNER)
        }
    }
}
