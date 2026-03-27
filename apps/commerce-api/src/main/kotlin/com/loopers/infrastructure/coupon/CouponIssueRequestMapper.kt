package com.loopers.infrastructure.coupon

import com.loopers.domain.coupon.CouponIssueRequest
import org.springframework.stereotype.Component

@Component
class CouponIssueRequestMapper {
    fun toDomain(entity: CouponIssueRequestEntity): CouponIssueRequest {
        return CouponIssueRequest.retrieve(
            id = entity.id!!,
            couponId = entity.couponId,
            userId = entity.userId,
            status = CouponIssueRequest.Status.valueOf(entity.status),
            failureReasonCode = entity.failureReasonCode,
            issuedCouponId = entity.issuedCouponId,
        )
    }

    fun toEntity(request: CouponIssueRequest): CouponIssueRequestEntity {
        return CouponIssueRequestEntity(
            id = request.id,
            couponId = request.couponId,
            userId = request.userId,
            status = request.status.name,
            failureReasonCode = request.failureReasonCode,
            issuedCouponId = request.issuedCouponId,
        )
    }
}
