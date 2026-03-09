package com.loopers.application.coupon

import com.loopers.domain.coupon.CouponType
import java.math.BigDecimal
import java.time.ZonedDateTime

data class CreateCouponCriteria(
    val name: String,
    val type: CouponType,
    val value: BigDecimal,
    val minOrderAmount: BigDecimal?,
    val expiredAt: ZonedDateTime,
)

data class UpdateCouponCriteria(
    val name: String,
    val value: BigDecimal,
    val minOrderAmount: BigDecimal?,
    val expiredAt: ZonedDateTime,
)
