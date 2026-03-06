package com.loopers.domain.coupon

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import java.time.ZonedDateTime

class IssuedCoupon private constructor(
    val id: Long?,
    val couponId: Long,
    val userId: Long,
    val status: Status,
    val expiredAt: ZonedDateTime,
    val usedAt: ZonedDateTime?,
) {
    fun use(): IssuedCoupon {
        if (status == Status.USED) {
            throw CoreException(ErrorType.ISSUED_COUPON_ALREADY_USED)
        }
        if (!ZonedDateTime.now().isBefore(expiredAt)) {
            throw CoreException(ErrorType.ISSUED_COUPON_EXPIRED)
        }
        return IssuedCoupon(
            id = id,
            couponId = couponId,
            userId = userId,
            status = Status.USED,
            expiredAt = expiredAt,
            usedAt = ZonedDateTime.now(),
        )
    }

    fun isUsable(): Boolean = status == Status.AVAILABLE && ZonedDateTime.now().isBefore(expiredAt)

    fun displayStatus(): DisplayStatus = when {
        status == Status.USED -> DisplayStatus.USED
        !ZonedDateTime.now().isBefore(expiredAt) -> DisplayStatus.EXPIRED
        else -> DisplayStatus.AVAILABLE
    }

    enum class Status {
        AVAILABLE,
        USED,
    }

    enum class DisplayStatus {
        AVAILABLE,
        USED,
        EXPIRED,
    }

    companion object {
        fun issue(
            couponId: Long,
            userId: Long,
            expiredAt: ZonedDateTime,
        ): IssuedCoupon = IssuedCoupon(
            id = null,
            couponId = couponId,
            userId = userId,
            status = Status.AVAILABLE,
            expiredAt = expiredAt,
            usedAt = null,
        )

        fun retrieve(
            id: Long,
            couponId: Long,
            userId: Long,
            status: Status,
            expiredAt: ZonedDateTime,
            usedAt: ZonedDateTime?,
        ): IssuedCoupon = IssuedCoupon(
            id = id,
            couponId = couponId,
            userId = userId,
            status = status,
            expiredAt = expiredAt,
            usedAt = usedAt,
        )
    }
}
