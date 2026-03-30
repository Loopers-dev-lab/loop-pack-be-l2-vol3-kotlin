package com.loopers.domain.coupon

import com.loopers.domain.BaseEntity
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table

@Entity
@Table(name = "coupon_issue_requests")
class CouponIssueRequest(
    requestId: String,
    couponId: Long,
    userId: Long,
) : BaseEntity() {

    @Column(name = "request_id", nullable = false, unique = true)
    var requestId: String = requestId
        protected set

    @Column(name = "coupon_id", nullable = false)
    var couponId: Long = couponId
        protected set

    @Column(name = "user_id", nullable = false)
    var userId: Long = userId
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: CouponIssueStatus = CouponIssueStatus.PENDING
        protected set

    @Column(name = "fail_reason")
    var failReason: String? = null
        protected set

    fun markIssued() {
        validatePending()
        status = CouponIssueStatus.ISSUED
    }

    fun markFailed(reason: String) {
        validatePending()
        status = CouponIssueStatus.FAILED
        failReason = reason
    }

    private fun validatePending() {
        if (status != CouponIssueStatus.PENDING) {
            throw CoreException(ErrorType.CONFLICT, "PENDING 상태에서만 상태를 변경할 수 있습니다. 현재 상태: $status")
        }
    }
}
