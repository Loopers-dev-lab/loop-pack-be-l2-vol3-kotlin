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

    fun use() {
        validateUsable()
        this.status = CouponStatus.USED
    }

    fun restore() {
        if (status != CouponStatus.USED) {
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
