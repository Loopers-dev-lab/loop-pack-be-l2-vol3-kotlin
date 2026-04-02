package com.loopers.domain.coupon

import java.time.ZonedDateTime

data class CouponDto(
    val id: Long,
    val userId: Long,
    val templateId: Long,
    val status: CouponStatus,
    val requestedAt: ZonedDateTime,
    val usedAt: ZonedDateTime? = null,
)
