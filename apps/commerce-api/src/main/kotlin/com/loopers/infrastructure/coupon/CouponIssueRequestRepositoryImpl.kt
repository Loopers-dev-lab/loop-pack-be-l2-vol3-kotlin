package com.loopers.infrastructure.coupon

import com.loopers.domain.coupon.CouponIssueRequest
import com.loopers.domain.coupon.CouponIssueRequestRepository
import org.springframework.stereotype.Component

@Component
class CouponIssueRequestRepositoryImpl(
    private val jpaRepository: CouponIssueRequestJpaRepository,
) : CouponIssueRequestRepository {

    override fun save(request: CouponIssueRequest): CouponIssueRequest {
        return jpaRepository.save(request)
    }

    override fun findByRequestId(requestId: String): CouponIssueRequest? {
        return jpaRepository.findByRequestId(requestId)
    }

    override fun findByUserIdAndCouponId(userId: Long, couponId: Long): CouponIssueRequest? {
        return jpaRepository.findByUserIdAndCouponId(userId, couponId)
    }
}
