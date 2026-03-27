package com.loopers.application.user.coupon

import com.loopers.domain.coupon.CouponIssueRequestRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserCouponIssueRequestStatusService(
    private val couponIssueRequestRepository: CouponIssueRequestRepository,
) : UserCouponIssueRequestStatusUseCase {
    @Transactional(readOnly = true)
    override fun getStatus(command: UserCouponCommand.IssueRequestStatus): UserCouponResult.IssueRequestStatus {
        val request = couponIssueRequestRepository.findByIdAndUserId(command.requestId, command.userId)
            ?: throw CoreException(ErrorType.COUPON_ISSUE_REQUEST_NOT_FOUND)
        return UserCouponResult.IssueRequestStatus.from(request)
    }
}
