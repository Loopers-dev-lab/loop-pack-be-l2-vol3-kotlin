package com.loopers.domain.coupon

import com.loopers.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.ZonedDateTime
import java.util.UUID

@Entity
@Table(
    name = "coupon_issue_requests",
    indexes = [Index(name = "idx_issue_req_user_coupon", columnList = "user_id, coupon_id")],
)
class CouponIssueRequest private constructor(
    requestId: String,
    couponId: Long,
    userId: Long,
    status: CouponIssueStatus,
) : BaseEntity() {

    @Column(name = "request_id", nullable = false, unique = true, length = 36)
    val requestId: String = requestId

    @Column(name = "coupon_id", nullable = false)
    val couponId: Long = couponId

    @Column(name = "user_id", nullable = false)
    val userId: Long = userId

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: CouponIssueStatus = status
        protected set

    @Column(name = "reject_reason", length = 200)
    var rejectReason: String? = null
        protected set

    @Column(name = "processed_at")
    var processedAt: ZonedDateTime? = null
        protected set

    fun markIssued() {
        this.status = CouponIssueStatus.ISSUED
        this.processedAt = ZonedDateTime.now()
    }

    fun markRejected(reason: String) {
        this.status = CouponIssueStatus.REJECTED
        this.rejectReason = reason
        this.processedAt = ZonedDateTime.now()
    }

    companion object {
        fun create(
            couponId: Long,
            userId: Long,
        ): CouponIssueRequest {
            return CouponIssueRequest(
                requestId = UUID.randomUUID().toString(),
                couponId = couponId,
                userId = userId,
                status = CouponIssueStatus.PENDING,
            )
        }
    }
}
