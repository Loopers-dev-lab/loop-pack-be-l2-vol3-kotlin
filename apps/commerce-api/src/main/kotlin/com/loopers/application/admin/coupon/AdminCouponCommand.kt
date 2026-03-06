package com.loopers.application.admin.coupon

import java.math.BigDecimal
import java.time.ZonedDateTime

class AdminCouponCommand {
    data class Register(
        val name: String,
        val type: String,
        val discountValue: Long,
        val minOrderAmount: BigDecimal?,
        val expiredAt: ZonedDateTime,
        val admin: String,
    )

    data class Update(
        val couponId: Long,
        val name: String,
        val discountValue: Long,
        val minOrderAmount: BigDecimal?,
        val expiredAt: ZonedDateTime,
        val admin: String,
    )
}
