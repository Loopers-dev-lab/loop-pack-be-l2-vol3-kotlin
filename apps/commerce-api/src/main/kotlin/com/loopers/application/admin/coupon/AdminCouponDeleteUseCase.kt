package com.loopers.application.admin.coupon

import com.loopers.domain.coupon.CouponRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AdminCouponDeleteUseCase(
    private val couponRepository: CouponRepository,
) {
    @Transactional
    fun delete(couponId: Long) {
        val coupon = couponRepository.findById(couponId)
            ?: throw CoreException(ErrorType.COUPON_NOT_FOUND)

        val deleted = coupon.delete()
        couponRepository.save(deleted)
    }
}
