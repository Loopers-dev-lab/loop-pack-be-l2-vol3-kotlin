package com.loopers.application.coupon

import com.loopers.domain.coupon.CouponRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class DeleteCouponUseCase(
    private val couponRepository: CouponRepository,
) {
    @Transactional
    fun delete(id: Long) {
        val coupon = couponRepository.findById(id)
            ?: throw CoreException(ErrorType.NOT_FOUND, "쿠폰을 찾을 수 없습니다: $id")

        if (coupon.isDeleted()) {
            return
        }

        val deletedCoupon = coupon.delete()
        couponRepository.save(deletedCoupon)
    }
}
