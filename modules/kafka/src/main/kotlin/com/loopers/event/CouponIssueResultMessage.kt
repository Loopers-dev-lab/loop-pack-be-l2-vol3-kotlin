package com.loopers.event

import java.time.ZonedDateTime

data class CouponIssueResultMessage(
    val requestId: String,
    val couponId: Long,
    val userId: Long,
    val status: String,
    val failureReason: String?,
    val processedAt: ZonedDateTime,
)
