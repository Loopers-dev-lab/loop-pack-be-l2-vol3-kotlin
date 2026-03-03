package com.loopers.application.coupon

import com.loopers.domain.common.vo.UserId
import com.loopers.domain.coupon.repository.CouponRepository
import com.loopers.domain.coupon.repository.IssuedCouponRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class GetMyCouponsUseCase(
    private val couponRepository: CouponRepository,
    private val issuedCouponRepository: IssuedCouponRepository,
) {

    @Transactional(readOnly = true)
    fun execute(userId: Long): List<MyCouponInfo> {
        val issuedCoupons = issuedCouponRepository.findAllByRefUserId(UserId(userId))
        val couponIds = issuedCoupons.map { it.refCouponId }.distinct()
        val coupons = couponRepository.findAllByIds(couponIds).associateBy { it.id }
        return issuedCoupons.map { issuedCoupon ->
            val coupon = coupons[issuedCoupon.refCouponId]
                ?: throw CoreException(ErrorType.NOT_FOUND, "쿠폰을 찾을 수 없습니다.")
            MyCouponInfo.from(issuedCoupon, coupon)
        }
    }
}
