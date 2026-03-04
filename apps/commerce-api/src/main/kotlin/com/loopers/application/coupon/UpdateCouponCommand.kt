package com.loopers.application.coupon

import java.time.ZonedDateTime

data class UpdateCouponCommand(
    val name: String,
    val discountType: String,
    val discountValue: Long,
    val minOrderAmount: Long,
    val maxIssueCount: Int?,
    val expiredAt: ZonedDateTime,
)
