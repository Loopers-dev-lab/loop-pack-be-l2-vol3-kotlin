package com.loopers.application.coupon

import com.loopers.domain.common.vo.CouponId
import com.loopers.domain.coupon.repository.CouponRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class GetCouponAdminUseCase(
    private val couponRepository: CouponRepository,
) {
    @Transactional(readOnly = true)
    fun execute(couponId: Long): CouponInfo {
        val coupon = couponRepository.findById(CouponId(couponId))
            ?: throw CoreException(ErrorType.NOT_FOUND, "쿠폰을 찾을 수 없습니다.")
        return CouponInfo.from(coupon)
    }
}
