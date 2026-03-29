package com.loopers.infrastructure.coupon

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import java.time.ZonedDateTime

@Entity
@Table(name = "coupon_issue_requests")
class CouponIssueRequestEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "coupon_id", nullable = false)
    val couponId: Long,

    @Column(name = "member_id", nullable = false)
    val memberId: Long,

    @Column(name = "status", nullable = false, length = 40)
    var status: String,

    @Column(name = "issued_coupon_id")
    var issuedCouponId: Long? = null,

    @Column(name = "failure_reason", length = 255)
    var failureReason: String? = null,

    @Column(name = "requested_at", nullable = false)
    val requestedAt: ZonedDateTime,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: ZonedDateTime? = null,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: ZonedDateTime? = null,
) {
    @PrePersist
    fun prePersist() {
        val now = ZonedDateTime.now()
        createdAt = now
        updatedAt = now
    }

    @PreUpdate
    fun preUpdate() {
        updatedAt = ZonedDateTime.now()
    }
}
