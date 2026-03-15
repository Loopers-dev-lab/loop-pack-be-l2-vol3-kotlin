package com.loopers.application.coupon

import com.loopers.domain.coupon.CouponException
import com.loopers.domain.coupon.CouponError
import com.loopers.domain.coupon.CouponRepository
import com.loopers.domain.coupon.UserCoupon
import com.loopers.domain.coupon.UserCouponRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class IssueCouponUseCase(
    private val couponRepository: CouponRepository,
    private val userCouponRepository: UserCouponRepository,
) {
    @Transactional
    fun issue(userId: Long, couponId: Long): Long {
        val coupon = couponRepository.findByIdForUpdate(couponId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "쿠폰을 찾을 수 없습니다: $couponId")

        if (coupon.isDeleted()) {
            throw CouponException(CouponError.DELETED, "삭제된 쿠폰입니다.")
        }

        coupon.assertIssuable()

        if (userCouponRepository.existsByCouponIdAndUserId(couponId, userId)) {
            throw CouponException(CouponError.ALREADY_ISSUED, "이미 발급받은 쿠폰입니다.")
        }

        val userCoupon = UserCoupon.issue(coupon, userId)
        val userCouponId = userCouponRepository.save(userCoupon)

        couponRepository.incrementIssuedCount(couponId)

        return userCouponId
    }
}
