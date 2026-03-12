package com.loopers.application.coupon

import com.loopers.domain.coupon.CouponRepository
import com.loopers.domain.coupon.UserCoupon
import com.loopers.domain.coupon.UserCouponRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.CouponErrorCode
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class IssueCouponUseCase(
    private val couponRepository: CouponRepository,
    private val userCouponRepository: UserCouponRepository,
) {

    @Transactional
    fun execute(command: CouponCommand.Issue): UserCouponInfo {
        val coupon = couponRepository.findActiveByIdOrNull(command.couponId)
            ?: throw CoreException(CouponErrorCode.COUPON_NOT_FOUND)

        if (coupon.isExpired()) {
            throw CoreException(CouponErrorCode.COUPON_EXPIRED)
        }

        if (userCouponRepository.existsByUserIdAndCouponId(command.userId, command.couponId)) {
            throw CoreException(CouponErrorCode.ALREADY_ISSUED_COUPON)
        }

        val userCoupon = UserCoupon.create(
            couponId = coupon.id,
            userId = command.userId,
            expiredAt = coupon.expiredAt,
        )
        try {
            val saved = userCouponRepository.save(userCoupon)
            return UserCouponInfo.from(saved)
        } catch (e: DataIntegrityViolationException) {
            throw CoreException(CouponErrorCode.ALREADY_ISSUED_COUPON)
        }
    }
}
