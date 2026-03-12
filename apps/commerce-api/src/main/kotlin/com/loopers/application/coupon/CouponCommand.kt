package com.loopers.application.coupon

import com.loopers.domain.coupon.CouponType
import java.time.ZonedDateTime

class CouponCommand {

    data class Register(
        val name: String,
        val type: CouponType,
        val value: Long,
        val minOrderAmount: Long?,
        val expiredAt: ZonedDateTime,
    )

    data class Update(
        val couponId: Long,
        val name: String,
        val value: Long,
        val minOrderAmount: Long?,
        val expiredAt: ZonedDateTime,
    )

    data class Issue(
        val couponId: Long,
        val userId: Long,
    )
}
