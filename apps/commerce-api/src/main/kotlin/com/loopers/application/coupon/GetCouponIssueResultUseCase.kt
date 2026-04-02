package com.loopers.application.coupon

import com.loopers.domain.coupon.CouponIssueRequestRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.CouponErrorCode
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class GetCouponIssueResultUseCase(
    private val couponIssueRequestRepository: CouponIssueRequestRepository,
) {

    @Transactional(readOnly = true)
    fun execute(requestId: String): CouponIssueRequestInfo {
        val request = couponIssueRequestRepository.findByRequestIdOrNull(requestId)
            ?: throw CoreException(CouponErrorCode.COUPON_ISSUE_REQUEST_NOT_FOUND)
        return CouponIssueRequestInfo.from(request)
    }
}
