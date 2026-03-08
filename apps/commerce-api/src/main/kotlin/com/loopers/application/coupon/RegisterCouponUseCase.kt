package com.loopers.application.coupon

import com.loopers.domain.coupon.Coupon
import com.loopers.domain.coupon.CouponName
import com.loopers.domain.coupon.CouponRepository
import com.loopers.domain.coupon.DiscountType
import com.loopers.domain.product.Money
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class RegisterCouponUseCase(
    private val couponRepository: CouponRepository,
) {
    @Transactional
    fun register(command: RegisterCouponCommand): Long {
        val coupon = Coupon.create(
            name = CouponName(command.name),
            discountType = DiscountType.valueOf(command.discountType),
            discountValue = command.discountValue,
            minOrderAmount = Money(command.minOrderAmount),
            maxIssueCount = command.maxIssueCount,
            expiredAt = command.expiredAt,
        )
        return couponRepository.save(coupon)
    }
}
