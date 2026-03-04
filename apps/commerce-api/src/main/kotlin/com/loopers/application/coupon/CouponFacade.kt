package com.loopers.application.coupon

import com.loopers.domain.coupon.CouponService
import org.springframework.stereotype.Component

@Component
class CouponFacade(
    private val couponService: CouponService,
) {

    fun issue(couponId: Long, userId: Long) {
        couponService.issue(couponId, userId)
    }
}
