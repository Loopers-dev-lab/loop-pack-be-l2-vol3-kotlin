package com.loopers.application.coupon

import com.loopers.application.UseCase
import com.loopers.domain.coupon.CouponService
import org.springframework.stereotype.Component

@Component
class AdminGetCouponUseCase(
    private val couponService: CouponService,
) : UseCase<Long, GetCouponResult> {
    override fun execute(couponId: Long): GetCouponResult {
        val info = couponService.getCoupon(couponId)
        return GetCouponResult.from(info)
    }
}
