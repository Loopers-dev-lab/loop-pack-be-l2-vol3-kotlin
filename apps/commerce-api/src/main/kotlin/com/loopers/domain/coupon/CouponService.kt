package com.loopers.domain.coupon

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component

@Component
class CouponService(
    private val couponRepository: CouponRepository,
    private val issuedCouponRepository: IssuedCouponRepository,
) {

    fun issue(couponId: Long, userId: Long) {
        val coupon = couponRepository.findByIdWithLock(couponId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "쿠폰을 찾을 수 없습니다.")

        if (issuedCouponRepository.existsByCouponIdAndUserId(couponId, userId)) {
            throw CoreException(ErrorType.CONFLICT, "이미 발급받은 쿠폰입니다.")
        }

        coupon.issue()
        issuedCouponRepository.save(IssuedCoupon(couponId = couponId, userId = userId))
    }

    fun findIssuedCouponsByUserId(userId: Long): List<IssuedCoupon> {
        return issuedCouponRepository.findByUserId(userId)
    }

    fun findCouponsByIds(ids: List<Long>): List<Coupon> {
        return couponRepository.findByIdIn(ids)
    }

    fun findCouponById(couponId: Long): Coupon {
        return couponRepository.findById(couponId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "쿠폰을 찾을 수 없습니다.")
    }

    fun findIssuedCouponByCouponIdAndUserId(couponId: Long, userId: Long): IssuedCoupon {
        return issuedCouponRepository.findByCouponIdAndUserId(couponId, userId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "발급된 쿠폰을 찾을 수 없습니다.")
    }

    fun findIssuedCouponWithLock(couponId: Long, userId: Long): IssuedCoupon {
        return issuedCouponRepository.findByCouponIdAndUserIdWithLock(couponId, userId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "발급된 쿠폰을 찾을 수 없습니다.")
    }
}
