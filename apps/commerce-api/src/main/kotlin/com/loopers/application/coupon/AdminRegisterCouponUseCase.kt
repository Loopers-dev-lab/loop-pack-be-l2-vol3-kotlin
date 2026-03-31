package com.loopers.application.coupon

import com.loopers.application.UseCase
import com.loopers.domain.coupon.CouponService
import com.loopers.domain.coupon.RegisterCouponCommand
import com.loopers.infrastructure.coupon.CouponStockRedisRepository
import org.springframework.stereotype.Component

@Component
class AdminRegisterCouponUseCase(
    private val couponService: CouponService,
    private val couponStockRedisRepository: CouponStockRedisRepository,
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
        couponStockRedisRepository.initialize(info.id, criteria.totalQuantity)
        return GetCouponResult.from(info)
    }
}
