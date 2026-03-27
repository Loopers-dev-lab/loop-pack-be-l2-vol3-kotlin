package com.loopers.infrastructure.coupon

import com.loopers.domain.coupon.CouponIssueRequest
import com.loopers.domain.coupon.CouponIssueRequestRepository
import org.springframework.stereotype.Repository

@Repository
class CouponIssueRequestRepositoryImpl(
    private val couponIssueRequestJpaRepository: CouponIssueRequestJpaRepository,
) : CouponIssueRequestRepository {

    override fun save(request: CouponIssueRequest): CouponIssueRequest {
        return couponIssueRequestJpaRepository.save(request)
    }

    override fun findByRequestIdOrNull(requestId: String): CouponIssueRequest? {
        return couponIssueRequestJpaRepository.findByRequestId(requestId)
    }
}
