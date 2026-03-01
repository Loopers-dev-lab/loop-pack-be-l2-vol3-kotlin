package com.loopers.domain.coupon

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class CouponService(
    private val couponRepository: CouponRepository,
    private val issuedCouponRepository: IssuedCouponRepository,
) {

    @Transactional
    fun issue(couponId: Long, userId: Long) {
        val coupon = couponRepository.findByIdWithLock(couponId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "쿠폰을 찾을 수 없습니다.")

        if (issuedCouponRepository.existsByCouponIdAndUserId(couponId, userId)) {
            throw CoreException(ErrorType.CONFLICT, "이미 발급받은 쿠폰입니다.")
        }

        coupon.issue()
        issuedCouponRepository.save(IssuedCoupon(couponId = couponId, userId = userId))
    }
}
