package com.loopers.application.admin.coupon

import com.loopers.domain.common.Money
import com.loopers.domain.coupon.Coupon
import com.loopers.domain.coupon.CouponRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AdminCouponRegisterUseCase(
    private val couponRepository: CouponRepository,
) {
    @Transactional
    fun register(command: AdminCouponCommand.Register): AdminCouponResult.Register {
        val coupon = Coupon.register(
            name = command.name,
            type = Coupon.Type.valueOf(command.type),
            discountValue = command.discountValue,
            minOrderAmount = command.minOrderAmount?.let { Money(it) },
            expiredAt = command.expiredAt,
        )
        val saved = couponRepository.save(coupon)
        return AdminCouponResult.Register.from(saved)
    }
}
