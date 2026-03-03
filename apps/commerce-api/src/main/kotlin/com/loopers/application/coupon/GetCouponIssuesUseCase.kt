package com.loopers.application.coupon

import com.loopers.domain.coupon.CouponRepository
import com.loopers.domain.coupon.UserCouponRepository
import com.loopers.support.PageResult
import com.loopers.support.error.CoreException
import com.loopers.support.error.CouponErrorCode
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class GetCouponIssuesUseCase(
    private val couponRepository: CouponRepository,
    private val userCouponRepository: UserCouponRepository,
) {

    @Transactional(readOnly = true)
    fun execute(couponId: Long, page: Int, size: Int): PageResult<UserCouponInfo> {
        couponRepository.findActiveByIdOrNull(couponId)
            ?: throw CoreException(CouponErrorCode.COUPON_NOT_FOUND)

        val result = userCouponRepository.findAllByCouponId(couponId, page, size)
        return PageResult.of(
            content = result.content.map { UserCouponInfo.from(it) },
            page = result.page,
            size = result.size,
            totalElements = result.totalElements,
        )
    }
}
