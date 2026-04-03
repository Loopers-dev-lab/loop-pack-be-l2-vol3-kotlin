package com.loopers.infrastructure.coupon

import com.loopers.support.jpa.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.ZonedDateTime

@Entity
@Table(
    name = "coupon_issue_request",
    indexes = [
        Index(name = "idx_coupon_issue_request_id", columnList = "request_id", unique = true),
    ],
)
class CouponIssueRequestEntity(
    id: Long?,

    @Column(name = "request_id", nullable = false, unique = true, length = 36)
    val requestId: String,

    @Column(name = "coupon_id", nullable = false)
    val couponId: Long,

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(name = "status", nullable = false, length = 20)
    val status: String,

    @Column(name = "failure_reason", length = 500)
    val failureReason: String?,

    @Column(name = "processed_at")
    val processedAt: ZonedDateTime?,
) : BaseEntity() {
    init {
        this.id = id
    }
}
