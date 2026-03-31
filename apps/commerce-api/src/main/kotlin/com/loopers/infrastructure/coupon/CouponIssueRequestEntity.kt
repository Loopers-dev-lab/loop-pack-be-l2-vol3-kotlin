package com.loopers.infrastructure.coupon

import com.loopers.infrastructure.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table

@Table(
    name = "coupon_issue_request",
    indexes = [
        Index(name = "uk_coupon_issue_request_coupon_user", columnList = "coupon_id, user_id", unique = true),
        Index(name = "idx_coupon_issue_request_coupon_id", columnList = "coupon_id"),
        Index(name = "idx_coupon_issue_request_user_id", columnList = "user_id"),
    ],
)
@Entity
class CouponIssueRequestEntity(
    id: Long? = null,
    @Column(name = "coupon_id", nullable = false)
    val couponId: Long,
    @Column(name = "user_id", nullable = false)
    val userId: Long,
    @Column(nullable = false)
    var status: String,
    @Column(name = "failure_reason_code")
    var failureReasonCode: String?,
    @Column(name = "issued_coupon_id")
    var issuedCouponId: Long?,
) : BaseEntity() {
    init {
        this.id = id
    }
}
