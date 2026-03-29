package com.loopers.infrastructure.coupon

import com.loopers.domain.coupon.CouponIssueRequest
import com.loopers.domain.coupon.CouponIssueRequestStatus
import org.springframework.stereotype.Component

@Component
class CouponIssueRequestMapper {
    fun toDomain(entity: CouponIssueRequestEntity): CouponIssueRequest {
        return CouponIssueRequest(
            id = entity.id,
            couponId = entity.couponId,
            memberId = entity.memberId,
            status = CouponIssueRequestStatus.valueOf(entity.status),
            requestedAt = entity.requestedAt,
            issuedCouponId = entity.issuedCouponId,
            failureReason = entity.failureReason,
        )
    }

    fun toEntity(domain: CouponIssueRequest): CouponIssueRequestEntity {
        return CouponIssueRequestEntity(
            couponId = domain.couponId,
            memberId = domain.memberId,
            status = domain.status.name,
            issuedCouponId = domain.issuedCouponId,
            failureReason = domain.failureReason,
            requestedAt = domain.requestedAt,
        )
    }

    fun update(entity: CouponIssueRequestEntity, domain: CouponIssueRequest) {
        entity.status = domain.status.name
        entity.issuedCouponId = domain.issuedCouponId
        entity.failureReason = domain.failureReason
    }
}
