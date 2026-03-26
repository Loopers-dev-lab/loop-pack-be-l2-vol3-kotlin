package com.loopers.application.coupon

import com.loopers.application.UseCase
import com.loopers.domain.coupon.CouponIssueRequestRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class UserGetCouponIssueStatusUseCase(
    private val couponIssueRequestRepository: CouponIssueRequestRepository,
) : UseCase<GetCouponIssueStatusCriteria, CouponIssueStatusResult> {

    @Transactional(readOnly = true)
    override fun execute(criteria: GetCouponIssueStatusCriteria): CouponIssueStatusResult {
        val request = couponIssueRequestRepository.findById(criteria.requestId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "쿠폰 발급 요청을 찾을 수 없습니다.")
        return CouponIssueStatusResult.from(request)
    }
}
