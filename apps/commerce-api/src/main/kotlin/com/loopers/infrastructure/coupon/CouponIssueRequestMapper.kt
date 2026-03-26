package com.loopers.infrastructure.coupon

import com.loopers.domain.coupon.CouponIssueRequest

object CouponIssueRequestMapper {
    fun toDomain(entity: CouponIssueRequestEntity): CouponIssueRequest {
        val id = requireNotNull(entity.id) { "CouponIssueRequestEntity.id가 null입니다." }
        return CouponIssueRequest.reconstitute(
            persistenceId = id,
            requestId = entity.requestId,
            refCouponId = entity.couponId,
            refUserId = entity.userId,
            status = entity.status,
            failureReason = entity.failureReason,
            processedAt = entity.processedAt,
        )
    }

    fun toEntity(domain: CouponIssueRequest): CouponIssueRequestEntity {
        return CouponIssueRequestEntity(
            id = domain.persistenceId,
            requestId = domain.requestId,
            couponId = domain.refCouponId,
            userId = domain.refUserId,
            status = domain.status,
            failureReason = domain.failureReason,
            processedAt = domain.processedAt,
        )
    }
}
