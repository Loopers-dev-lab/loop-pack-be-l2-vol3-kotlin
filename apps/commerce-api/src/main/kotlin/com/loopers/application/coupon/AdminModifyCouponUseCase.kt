package com.loopers.application.coupon

import com.loopers.application.UseCase
import com.loopers.domain.coupon.CouponService
import com.loopers.domain.coupon.ModifyCouponCommand
import org.springframework.stereotype.Component

@Component
class AdminModifyCouponUseCase(
    private val couponService: CouponService,
) : UseCase<ModifyCouponCriteria, GetCouponResult> {
    override fun execute(criteria: ModifyCouponCriteria): GetCouponResult {
        val command = ModifyCouponCommand(
            name = criteria.name,
            discountType = criteria.discountType,
            discountValue = criteria.discountValue,
            totalQuantity = criteria.totalQuantity,
            expiredAt = criteria.expiredAt,
        )
        val info = couponService.modify(criteria.couponId, command)
        return GetCouponResult.from(info)
    }
}
