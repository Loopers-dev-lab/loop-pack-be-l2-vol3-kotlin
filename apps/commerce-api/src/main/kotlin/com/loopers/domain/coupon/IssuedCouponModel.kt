package com.loopers.domain.coupon

import com.loopers.domain.error.CoreException
import com.loopers.domain.error.ErrorType
import java.time.ZonedDateTime

data class IssuedCouponModel(
    val id: Long = 0,
    val couponTemplateId: Long,
    val memberId: Long,
    val status: CouponStatus = CouponStatus.AVAILABLE,
    val expiredAt: ZonedDateTime,
    val usedAt: ZonedDateTime? = null,
    val version: Long = 0,
    val createdAt: ZonedDateTime? = null,
    val updatedAt: ZonedDateTime? = null,
) {
    fun use(): IssuedCouponModel {
        if (status != CouponStatus.AVAILABLE) {
            throw CoreException(ErrorType.BAD_REQUEST, "사용 불가능한 쿠폰입니다.")
        }
        if (isExpired()) {
            throw CoreException(ErrorType.BAD_REQUEST, "만료된 쿠폰입니다.")
        }
        return copy(status = CouponStatus.USED, usedAt = ZonedDateTime.now())
    }

    fun isExpired(): Boolean = expiredAt.isBefore(ZonedDateTime.now())

    fun isAvailable(): Boolean = status == CouponStatus.AVAILABLE && !isExpired()

    fun restore(): IssuedCouponModel {
        if (status != CouponStatus.USED) {
            throw CoreException(ErrorType.BAD_REQUEST, "사용된 쿠폰만 복원할 수 있습니다.")
        }
        return copy(status = CouponStatus.AVAILABLE, usedAt = null)
    }
}
