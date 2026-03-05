package com.loopers.application.coupon

import com.loopers.application.UseCase
import com.loopers.domain.coupon.CouponService
import org.springframework.stereotype.Component

@Component
class AdminDeleteCouponUseCase(
    private val couponService: CouponService,
) : UseCase<Long, Unit> {
    override fun execute(couponId: Long) {
        couponService.delete(couponId)
    }
}
