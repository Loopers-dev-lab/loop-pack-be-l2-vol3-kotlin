package com.loopers.application.admin.coupon

import com.loopers.domain.coupon.CouponRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AdminCouponDetailUseCase(
    private val couponRepository: CouponRepository,
) {
    @Transactional(readOnly = true)
    fun getDetail(couponId: Long): AdminCouponResult.Detail {
        val coupon = couponRepository.findById(couponId)
            ?: throw CoreException(ErrorType.COUPON_NOT_FOUND)

        return AdminCouponResult.Detail.from(coupon)
    }
}
