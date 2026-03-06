package com.loopers.domain.coupon

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component
import java.time.ZonedDateTime

@Component
class CouponIssuer(
    private val couponReader: CouponReader,
    private val issuedCouponRepository: IssuedCouponRepository,
) {

    fun issue(couponId: Long, memberId: Long): IssuedCoupon {
        val coupon = couponReader.getById(couponId)
        coupon.validateIssuable()

        if (issuedCouponRepository.existsByCouponIdAndMemberId(couponId, memberId)) {
            throw CoreException(ErrorType.DUPLICATE_COUPON_ISSUE)
        }

        val issuedCoupon = IssuedCoupon(
            couponId = couponId,
            memberId = memberId,
            issuedAt = ZonedDateTime.now(),
        )

        return issuedCouponRepository.save(issuedCoupon)
    }
}
