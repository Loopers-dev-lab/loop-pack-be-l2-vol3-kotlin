package com.loopers.domain.coupon

import com.loopers.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table

@Entity
@Table(name = "coupon_issue_request")
class CouponIssueRequestModel(
    couponId: Long,
    userId: Long,
    status: CouponIssueRequestStatus = CouponIssueRequestStatus.ACCEPTED,
) : BaseEntity() {
    @Column(name = "coupon_id", nullable = false)
    var couponId: Long = couponId
        protected set

    @Column(name = "user_id", nullable = false)
    var userId: Long = userId
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: CouponIssueRequestStatus = status
        protected set

    @Column(name = "coupon_issue_id")
    var couponIssueId: Long? = null
        protected set

    fun markProcessing() {
        if (status == CouponIssueRequestStatus.ACCEPTED) {
            status = CouponIssueRequestStatus.PROCESSING
        }
    }

    fun markCompleted(couponIssueId: Long) {
        this.couponIssueId = couponIssueId
        this.status = CouponIssueRequestStatus.COMPLETED
    }

    fun markDuplicate() {
        this.status = CouponIssueRequestStatus.DUPLICATE
    }

    fun markSoldOut() {
        this.status = CouponIssueRequestStatus.SOLD_OUT
    }

    fun markExpired() {
        this.status = CouponIssueRequestStatus.EXPIRED
    }

    fun isFinalStatus(): Boolean {
        return status == CouponIssueRequestStatus.COMPLETED ||
            status == CouponIssueRequestStatus.DUPLICATE ||
            status == CouponIssueRequestStatus.SOLD_OUT ||
            status == CouponIssueRequestStatus.EXPIRED
    }
}
