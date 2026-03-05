package com.loopers.application.coupon

import com.loopers.application.UseCase
import com.loopers.domain.coupon.CouponService
import com.loopers.domain.coupon.RegisterCouponCommand
import org.springframework.stereotype.Component

@Component
class AdminRegisterCouponUseCase(
    private val couponService: CouponService,
) : UseCase<RegisterCouponCriteria, GetCouponResult> {
    override fun execute(criteria: RegisterCouponCriteria): GetCouponResult {
        val command = RegisterCouponCommand(
            name = criteria.name,
            discountType = criteria.discountType,
            discountValue = criteria.discountValue,
            totalQuantity = criteria.totalQuantity,
            expiredAt = criteria.expiredAt,
        )
        val info = couponService.register(command)
        return GetCouponResult.from(info)
    }
}
