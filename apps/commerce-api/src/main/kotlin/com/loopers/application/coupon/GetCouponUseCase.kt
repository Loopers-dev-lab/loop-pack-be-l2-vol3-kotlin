package com.loopers.application.coupon

import com.loopers.domain.coupon.CouponRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.CouponErrorCode
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class GetCouponUseCase(
    private val couponRepository: CouponRepository,
) {

    @Transactional(readOnly = true)
    fun execute(couponId: Long): CouponInfo {
        val coupon = couponRepository.findActiveByIdOrNull(couponId)
            ?: throw CoreException(CouponErrorCode.COUPON_NOT_FOUND)

        return CouponInfo.from(coupon)
    }
}
