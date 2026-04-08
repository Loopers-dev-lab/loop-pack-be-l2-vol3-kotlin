package com.loopers.domain.coupon

import com.loopers.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Index
import jakarta.persistence.Table

@Entity
@Table(
    name = "coupon_issue_requests",
    indexes = [
        Index(name = "idx_coupon_issue_req_user_template", columnList = "user_id, coupon_template_id", unique = true),
        Index(name = "idx_coupon_issue_req_request_id", columnList = "request_id", unique = true),
    ],
)
class CouponIssueRequest(
    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(name = "coupon_template_id", nullable = false)
    val couponTemplateId: Long,

    @Column(name = "request_id", nullable = false, unique = true)
    val requestId: String,
) : BaseEntity() {
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: CouponIssueStatus = CouponIssueStatus.PENDING
        protected set

    @Column(name = "reason")
    var reason: String? = null
        protected set

    fun complete() {
        status = CouponIssueStatus.COMPLETED
    }

    fun fail(reason: String) {
        status = CouponIssueStatus.FAILED
        this.reason = reason
    }
}

enum class CouponIssueStatus {
    PENDING,
    COMPLETED,
    FAILED,
}
