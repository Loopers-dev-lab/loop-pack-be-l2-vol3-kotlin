package com.loopers.domain.coupon

import com.loopers.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "coupon_issue_requests",
    uniqueConstraints = [UniqueConstraint(name = "uk_coupon_user", columnNames = ["coupon_id", "user_id"])],
)
class CouponIssueRequestModel(
    couponId: Long,
    userId: Long,
) : BaseEntity() {

    @Column(name = "coupon_id", nullable = false)
    var couponId: Long = couponId
        protected set

    @Column(name = "user_id", nullable = false)
    var userId: Long = userId
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: CouponIssueRequestStatus = CouponIssueRequestStatus.PENDING
        protected set

    @Column(name = "failure_reason", length = 200)
    var failureReason: String? = null
        protected set

    fun markSuccess() {
        status = CouponIssueRequestStatus.SUCCESS
    }

    fun markFailed(reason: String) {
        status = CouponIssueRequestStatus.FAILED
        failureReason = reason
    }
}
