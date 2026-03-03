package com.loopers.domain.coupon.service

import com.loopers.domain.common.vo.Money
import com.loopers.domain.common.vo.UserId
import com.loopers.domain.coupon.model.Coupon
import com.loopers.domain.coupon.model.IssuedCoupon
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component

@Component
class CouponValidator {

    fun validateForOrder(
        issuedCoupon: IssuedCoupon,
        coupon: Coupon,
        userId: UserId,
        orderAmount: Money,
    ) {
        if (!issuedCoupon.isOwnedBy(userId)) {
            throw CoreException(ErrorType.BAD_REQUEST, "본인 소유의 쿠폰이 아닙니다.")
        }

        if (!issuedCoupon.isAvailable()) {
            throw CoreException(ErrorType.BAD_REQUEST, "사용할 수 없는 쿠폰입니다.")
        }

        if (coupon.isExpired()) {
            throw CoreException(ErrorType.BAD_REQUEST, "만료된 쿠폰입니다.")
        }

        val minOrderAmount = coupon.minOrderAmount
        if (minOrderAmount != null && orderAmount.value < minOrderAmount.value) {
            throw CoreException(ErrorType.BAD_REQUEST, "최소 주문 금액을 충족하지 않습니다.")
        }
    }
}
