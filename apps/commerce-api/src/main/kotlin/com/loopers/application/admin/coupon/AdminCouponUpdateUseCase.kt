package com.loopers.application.admin.coupon

import com.loopers.domain.common.Money
import com.loopers.domain.coupon.CouponRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AdminCouponUpdateUseCase(
    private val couponRepository: CouponRepository,
) {
    @Transactional
    fun update(command: AdminCouponCommand.Update): AdminCouponResult.Update {
        val coupon = couponRepository.findById(command.couponId)
            ?: throw CoreException(ErrorType.COUPON_NOT_FOUND)

        val updated = coupon.update(
            name = command.name,
            discountValue = command.discountValue,
            minOrderAmount = command.minOrderAmount?.let { Money(it) },
            expiredAt = command.expiredAt,
        )
        val saved = couponRepository.save(updated)
        return AdminCouponResult.Update.from(saved)
    }
}
