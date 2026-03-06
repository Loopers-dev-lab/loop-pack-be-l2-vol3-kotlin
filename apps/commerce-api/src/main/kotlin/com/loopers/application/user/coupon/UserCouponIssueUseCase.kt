package com.loopers.application.user.coupon

import com.loopers.domain.coupon.CouponRepository
import com.loopers.domain.coupon.IssuedCoupon
import com.loopers.domain.coupon.IssuedCouponRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserCouponIssueUseCase(
    private val couponRepository: CouponRepository,
    private val issuedCouponRepository: IssuedCouponRepository,
) {
    @Transactional
    fun issue(command: UserCouponCommand.Issue): UserCouponResult.Issued {
        val coupon = couponRepository.findById(command.couponId)
            ?: throw CoreException(ErrorType.COUPON_NOT_FOUND)

        if (coupon.isExpired()) {
            throw CoreException(ErrorType.COUPON_EXPIRED)
        }

        val issuedCoupon = IssuedCoupon.issue(
            couponId = coupon.id!!,
            userId = command.userId,
            expiredAt = coupon.expiredAt,
        )
        val saved = issuedCouponRepository.save(issuedCoupon)
        return UserCouponResult.Issued.from(saved)
    }
}
