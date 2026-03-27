package com.loopers.domain.coupon

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class CouponIssueRequestService(
    private val couponIssueRequestRepository: CouponIssueRequestRepository,
) {

    @Transactional
    fun save(request: CouponIssueRequest): CouponIssueRequest {
        return couponIssueRequestRepository.save(request)
    }

    @Transactional(readOnly = true)
    fun findByRequestId(requestId: String): CouponIssueRequest {
        return couponIssueRequestRepository.findByRequestId(requestId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "존재하지 않는 발급 요청입니다.")
    }

    @Transactional(readOnly = true)
    fun findByUserIdAndCouponId(userId: Long, couponId: Long): CouponIssueRequest? {
        return couponIssueRequestRepository.findByUserIdAndCouponId(userId, couponId)
    }
}
