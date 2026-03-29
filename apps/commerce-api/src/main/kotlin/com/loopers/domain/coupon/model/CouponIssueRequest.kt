package com.loopers.domain.coupon.model

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType

class CouponIssueRequest(
    val id: Long = 0,
    val requestId: String,
    val couponId: Long,
    val userId: Long,
    status: CouponIssueStatus = CouponIssueStatus.PENDING,
) {

    init {
        if (requestId.isBlank()) throw CoreException(ErrorType.BAD_REQUEST, "requestId는 필수입니다.")
        if (couponId <= 0) throw CoreException(ErrorType.BAD_REQUEST, "couponId는 양수여야 합니다.")
        if (userId <= 0) throw CoreException(ErrorType.BAD_REQUEST, "userId는 양수여야 합니다.")
    }

    var status: CouponIssueStatus = status
        private set

    enum class CouponIssueStatus {
        PENDING,
        SUCCESS,
        FAILED,
        SOLD_OUT,
        DUPLICATE,
    }

    fun markSuccess() {
        requirePending()
        status = CouponIssueStatus.SUCCESS
    }

    fun markFailed() {
        requirePending()
        status = CouponIssueStatus.FAILED
    }

    fun markSoldOut() {
        requirePending()
        status = CouponIssueStatus.SOLD_OUT
    }

    fun markDuplicate() {
        requirePending()
        status = CouponIssueStatus.DUPLICATE
    }

    private fun requirePending() {
        if (status != CouponIssueStatus.PENDING) {
            throw CoreException(ErrorType.BAD_REQUEST, "PENDING 상태에서만 변경할 수 있습니다.")
        }
    }
}
