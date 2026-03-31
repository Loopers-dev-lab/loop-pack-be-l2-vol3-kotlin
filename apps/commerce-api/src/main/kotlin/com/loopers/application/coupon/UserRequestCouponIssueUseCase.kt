package com.loopers.application.coupon

import com.loopers.application.UseCase
import com.loopers.domain.coupon.CouponIssueRequestModel
import com.loopers.domain.coupon.CouponIssueRequestRepository
import com.loopers.domain.user.UserService
import com.loopers.infrastructure.coupon.CouponStockRedisRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class UserRequestCouponIssueUseCase(
    private val userService: UserService,
    private val couponIssueRequestRepository: CouponIssueRequestRepository,
    private val couponStockRedisRepository: CouponStockRedisRepository,
) : UseCase<RequestCouponIssueCriteria, RequestCouponIssueResult> {

    @Transactional
    override fun execute(criteria: RequestCouponIssueCriteria): RequestCouponIssueResult {
        val user = userService.getUser(criteria.loginId)

        val existing = couponIssueRequestRepository.findByCouponIdAndUserId(criteria.couponId, user.id)
        if (existing != null) {
            throw CoreException(ErrorType.CONFLICT, "이미 쿠폰 발급을 요청했습니다.")
        }

        val result = couponStockRedisRepository.tryEnqueue(criteria.couponId, user.id)
        when (result) {
            0L -> throw CoreException(ErrorType.CONFLICT, "이미 쿠폰 발급을 요청했습니다.")
            -1L -> throw CoreException(ErrorType.BAD_REQUEST, "쿠폰 수량이 소진되었습니다.")
            -2L -> throw CoreException(ErrorType.BAD_REQUEST, "쿠폰 발급이 준비되지 않았습니다.")
        }

        val request = couponIssueRequestRepository.save(
            CouponIssueRequestModel(couponId = criteria.couponId, userId = user.id),
        )
        return RequestCouponIssueResult.from(request)
    }
}
