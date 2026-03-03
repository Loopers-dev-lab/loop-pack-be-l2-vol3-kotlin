package com.loopers.application.coupon

import com.loopers.domain.coupon.repository.CouponRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class DeleteCouponAdminUseCase(
    private val couponRepository: CouponRepository,
) {
    @Transactional
    fun execute(couponId: Long) {
        val coupon = couponRepository.findById(couponId) ?: return
        if (coupon.isDeleted()) return
        coupon.delete()
        couponRepository.save(coupon)
    }
}
