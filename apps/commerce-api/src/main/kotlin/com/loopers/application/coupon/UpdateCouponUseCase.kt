package com.loopers.application.coupon

import com.loopers.domain.coupon.CouponRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.CouponErrorCode
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class UpdateCouponUseCase(
    private val couponRepository: CouponRepository,
) {

    @Transactional
    fun execute(command: CouponCommand.Update): CouponInfo {
        val coupon = couponRepository.findActiveByIdOrNull(command.couponId)
            ?: throw CoreException(CouponErrorCode.COUPON_NOT_FOUND)

        coupon.update(
            name = command.name,
            value = command.value,
            minOrderAmount = command.minOrderAmount,
            expiredAt = command.expiredAt,
        )

        return CouponInfo.from(coupon)
    }
}
