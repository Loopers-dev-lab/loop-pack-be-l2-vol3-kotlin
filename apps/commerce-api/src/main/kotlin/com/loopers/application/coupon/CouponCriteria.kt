package com.loopers.application.coupon

import java.time.ZonedDateTime

data class CreateCouponCriteria(
    val name: String,
    val type: String,
    val value: Long,
    val expiredAt: ZonedDateTime,
)

data class UpdateCouponCriteria(
    val name: String,
    val type: String,
    val value: Long,
    val expiredAt: ZonedDateTime,
)
