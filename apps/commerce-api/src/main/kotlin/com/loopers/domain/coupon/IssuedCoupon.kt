package com.loopers.domain.coupon

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import java.time.ZonedDateTime

class IssuedCoupon(
    val id: Long? = null,
    val couponId: Long,
    val memberId: Long,
    status: CouponStatus = CouponStatus.AVAILABLE,
    val issuedAt: ZonedDateTime,
) {
    var status: CouponStatus = status
        private set

    fun reserve() {
        validateUsable()
        this.status = CouponStatus.RESERVED
    }

    fun confirmUse() {
        if (status != CouponStatus.RESERVED) {
            throw CoreException(ErrorType.COUPON_NOT_AVAILABLE)
        }
        this.status = CouponStatus.USED
    }

    fun release() {
        if (status != CouponStatus.RESERVED) {
            throw CoreException(ErrorType.COUPON_NOT_AVAILABLE)
        }
        this.status = CouponStatus.AVAILABLE
    }

    fun validateOwner(memberId: Long) {
        if (this.memberId != memberId) {
            throw CoreException(ErrorType.COUPON_NOT_OWNER)
        }
    }

    fun validateUsable() {
        if (status != CouponStatus.AVAILABLE) {
            throw CoreException(ErrorType.COUPON_NOT_AVAILABLE)
        }
    }
}
