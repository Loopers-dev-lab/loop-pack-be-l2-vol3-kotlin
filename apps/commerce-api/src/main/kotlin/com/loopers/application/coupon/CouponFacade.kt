package com.loopers.application.coupon

import com.loopers.domain.coupon.CouponService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class CouponFacade(
    private val couponService: CouponService,
) {

    @Transactional
    fun issue(couponId: Long, userId: Long) {
        couponService.issue(couponId, userId)
    }
}
