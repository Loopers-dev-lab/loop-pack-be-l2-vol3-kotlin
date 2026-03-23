package com.loopers.infrastructure.coupon

import com.loopers.domain.BaseEntity
import com.loopers.domain.coupon.model.CouponIssueRequest
import com.loopers.domain.coupon.model.CouponIssueRequest.CouponIssueStatus
import com.loopers.domain.withBaseFields
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "coupon_issue_requests",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_coupon_issue_requests_request_id", columnNames = ["request_id"]),
    ],
    indexes = [
        Index(name = "idx_coupon_issue_requests_coupon_user", columnList = "coupon_id, user_id"),
    ],
)
class CouponIssueRequestEntity(
    @Column(name = "request_id", nullable = false, length = 36)
    val requestId: String,
    @Column(name = "coupon_id", nullable = false)
    val couponId: Long,
    @Column(name = "user_id", nullable = false)
    val userId: Long,
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: CouponIssueStatus,
) : BaseEntity() {

    companion object {
        fun fromDomain(request: CouponIssueRequest): CouponIssueRequestEntity {
            return CouponIssueRequestEntity(
                requestId = request.requestId,
                couponId = request.couponId,
                userId = request.userId,
                status = request.status,
            ).withBaseFields(id = request.id)
        }
    }

    fun toDomain(): CouponIssueRequest = CouponIssueRequest(
        id = id,
        requestId = requestId,
        couponId = couponId,
        userId = userId,
        status = status,
    )
}
