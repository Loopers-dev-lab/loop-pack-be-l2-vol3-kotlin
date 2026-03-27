package com.loopers.domain.coupon

import java.time.ZonedDateTime

data class CouponIssuanceResultDto(
    val dedupeKey: String,
    val userId: Long,
    val templateId: Long,
    val status: IssuanceStatus = IssuanceStatus.PENDING,
    val couponId: Long? = null,
    val createdAt: ZonedDateTime = ZonedDateTime.now(),
    val updatedAt: ZonedDateTime? = null,
)
