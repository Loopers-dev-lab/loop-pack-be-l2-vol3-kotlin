package com.loopers.domain.coupon

import java.time.ZonedDateTime

class IssuedCoupon private constructor(
    val id: Long?,
    val couponId: Long,
    val userId: Long,
    val status: Status,
    val expiredAt: ZonedDateTime,
    val usedAt: ZonedDateTime?,
    val version: Long,
) {
    fun use(): IssuedCoupon {
        if (status == Status.USED) {
            throw IllegalStateException("issued coupon already used")
        }
        if (!ZonedDateTime.now().isBefore(expiredAt)) {
            throw IllegalStateException("issued coupon expired")
        }
        return IssuedCoupon(
            id = id,
            couponId = couponId,
            userId = userId,
            status = Status.USED,
            expiredAt = expiredAt,
            usedAt = ZonedDateTime.now(),
            version = version,
        )
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
            version = 0L,
        )

        fun retrieve(
            id: Long,
            couponId: Long,
            userId: Long,
            status: Status,
            expiredAt: ZonedDateTime,
            usedAt: ZonedDateTime?,
            version: Long,
        ): IssuedCoupon = IssuedCoupon(
            id = id,
            couponId = couponId,
            userId = userId,
            status = status,
            expiredAt = expiredAt,
            usedAt = usedAt,
            version = version,
        )
    }
}
