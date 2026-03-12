package com.loopers.application.coupon

import com.loopers.domain.coupon.Coupon
import com.loopers.domain.coupon.CouponRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class RegisterCouponUseCase(
    private val couponRepository: CouponRepository,
) {

    @Transactional
    fun execute(command: CouponCommand.Register): CouponInfo {
        val coupon = Coupon.create(
            name = command.name,
            type = command.type,
            value = command.value,
            minOrderAmount = command.minOrderAmount,
            expiredAt = command.expiredAt,
        )
        val saved = couponRepository.save(coupon)
        return CouponInfo.from(saved)
    }
}
