package com.loopers.domain.coupon

import java.time.ZonedDateTime

data class CreateCouponCommand(
    val name: String,
    val type: CouponType,
    val value: Long,
    val expiredAt: ZonedDateTime,
)

data class UpdateCouponCommand(
    val name: String,
    val type: CouponType,
    val value: Long,
    val expiredAt: ZonedDateTime,
)
