package com.loopers.domain.coupon

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component

@Component
class CouponIssueRequestReader(
    private val couponIssueRequestRepository: CouponIssueRequestRepository,
) {
    fun getById(id: Long): CouponIssueRequest {
        return couponIssueRequestRepository.findById(id)
            ?: throw CoreException(ErrorType.COUPON_ISSUE_REQUEST_NOT_FOUND)
    }
}
