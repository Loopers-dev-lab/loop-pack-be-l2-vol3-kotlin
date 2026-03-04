package com.loopers.application.coupon

import com.loopers.domain.coupon.CouponRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GetCouponUseCase(
    private val couponRepository: CouponRepository,
) {
    fun getById(id: Long): CouponInfo {
        val coupon = couponRepository.findById(id)
            ?: throw CoreException(ErrorType.NOT_FOUND, "쿠폰을 찾을 수 없습니다: $id")
        return CouponInfo.from(coupon)
    }
}
