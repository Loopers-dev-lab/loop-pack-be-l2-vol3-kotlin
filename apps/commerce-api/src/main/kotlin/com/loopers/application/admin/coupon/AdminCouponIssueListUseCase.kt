package com.loopers.application.admin.coupon

import com.loopers.domain.coupon.CouponRepository
import com.loopers.domain.coupon.IssuedCouponRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import com.loopers.support.page.PageRequest
import com.loopers.support.page.PageResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AdminCouponIssueListUseCase(
    private val couponRepository: CouponRepository,
    private val issuedCouponRepository: IssuedCouponRepository,
) {
    @Transactional(readOnly = true)
    fun getIssueList(
        couponId: Long,
        pageRequest: PageRequest,
    ): PageResponse<AdminCouponResult.IssuedCouponItem> {
        couponRepository.findById(couponId)
            ?: throw CoreException(ErrorType.COUPON_NOT_FOUND)

        return issuedCouponRepository.findAllByCouponId(couponId, pageRequest)
            .map { AdminCouponResult.IssuedCouponItem.from(it) }
    }
}
