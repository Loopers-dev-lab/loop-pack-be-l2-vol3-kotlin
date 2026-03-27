package com.loopers.domain.coupon

import java.time.ZonedDateTime

data class CouponInfo(
    val id: Long,
    val discountType: String,
    val discountValue: Long,
    val minOrderAmount: Long,
    val expiredAt: ZonedDateTime,
    val isDeleted: Boolean,
)
