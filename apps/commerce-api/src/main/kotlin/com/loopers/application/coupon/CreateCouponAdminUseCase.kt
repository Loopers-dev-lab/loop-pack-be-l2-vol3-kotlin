package com.loopers.application.coupon

import com.loopers.domain.common.vo.Money
import com.loopers.domain.coupon.model.Coupon
import com.loopers.domain.coupon.repository.CouponRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class CreateCouponAdminUseCase(
    private val couponRepository: CouponRepository,
) {
    @Transactional
    fun execute(command: CouponCommand.CreateCoupon): CouponInfo {
        val couponType = Coupon.CouponType.entries.find { it.name == command.type }
            ?: throw CoreException(ErrorType.BAD_REQUEST, "유효하지 않은 쿠폰 타입입니다: ${command.type}")

        val coupon = Coupon(
            name = command.name,
            type = couponType,
            value = command.value,
            maxDiscount = command.maxDiscount?.let { Money(it) },
            minOrderAmount = command.minOrderAmount?.let { Money(it) },
            totalQuantity = command.totalQuantity,
            expiredAt = command.expiredAt,
        )
        val saved = couponRepository.save(coupon)
        return CouponInfo.from(saved)
    }
}
