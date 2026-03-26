package com.loopers.domain.coupon

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.ZonedDateTime

@Entity
@Table(
    name = "coupon_issue_requests",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_coupon_issue_request_id", columnNames = ["request_id"]),
    ],
    indexes = [
        Index(name = "idx_coupon_issue_coupon_user", columnList = "coupon_id, user_id"),
    ],
)
class CouponIssueRequest(
    requestId: String,
    couponId: Long,
    userId: Long,
) {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0

    @Column(name = "request_id", nullable = false, unique = true)
    val requestId: String = requestId

    @Column(name = "coupon_id", nullable = false)
    val couponId: Long = couponId

    @Column(name = "user_id", nullable = false)
    val userId: Long = userId

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: CouponIssueRequestStatus = CouponIssueRequestStatus.PENDING
        private set

    @Column(name = "failure_reason", length = 500)
    var failureReason: String? = null
        private set

    @Column(name = "created_at", nullable = false, updatable = false)
    lateinit var createdAt: ZonedDateTime
        private set

    @Column(name = "updated_at", nullable = false)
    lateinit var updatedAt: ZonedDateTime
        private set

    @PrePersist
    private fun prePersist() {
        val now = ZonedDateTime.now()
        createdAt = now
        updatedAt = now
    }

    @PreUpdate
    private fun preUpdate() {
        updatedAt = ZonedDateTime.now()
    }

    fun markSuccess() {
        status = CouponIssueRequestStatus.SUCCESS
    }

    fun markFailed(reason: String?) {
        status = CouponIssueRequestStatus.FAILED
        failureReason = reason
    }
}

enum class CouponIssueRequestStatus {
    PENDING,
    SUCCESS,
    FAILED,
}
