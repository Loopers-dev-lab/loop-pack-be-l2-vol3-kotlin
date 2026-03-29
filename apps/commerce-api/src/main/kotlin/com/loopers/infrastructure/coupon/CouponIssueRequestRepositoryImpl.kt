package com.loopers.infrastructure.coupon

import com.loopers.domain.coupon.CouponIssueRequest
import com.loopers.domain.coupon.CouponIssueRequestRepository
import org.springframework.stereotype.Component

@Component
class CouponIssueRequestRepositoryImpl(
    private val couponIssueRequestJpaRepository: CouponIssueRequestJpaRepository,
) : CouponIssueRequestRepository {

    override fun save(couponIssueRequest: CouponIssueRequest): CouponIssueRequest {
        return couponIssueRequestJpaRepository.save(couponIssueRequest)
    }

    override fun findByRequestId(requestId: String): CouponIssueRequest? {
        return couponIssueRequestJpaRepository.findByRequestId(requestId)
    }

    override fun findByRequestIdAndUserId(requestId: String, userId: Long): CouponIssueRequest? {
        return couponIssueRequestJpaRepository.findByRequestIdAndUserId(requestId, userId)
    }
}
