package com.loopers.application.coupon

import com.loopers.domain.coupon.repository.CouponIssueRequestRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class GetCouponIssueStatusUseCase(
    private val couponIssueRequestRepository: CouponIssueRequestRepository,
) {

    @Transactional(readOnly = true)
    fun execute(requestId: String, userId: Long): CouponIssueStatusInfo {
        val request = couponIssueRequestRepository.findByRequestIdAndUserId(requestId, userId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "발급 요청을 찾을 수 없습니다.")
        return CouponIssueStatusInfo.from(request)
    }
}
