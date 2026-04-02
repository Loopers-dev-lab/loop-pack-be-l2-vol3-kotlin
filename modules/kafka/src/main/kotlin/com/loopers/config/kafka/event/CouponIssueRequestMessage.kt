package com.loopers.config.kafka.event

import java.time.ZonedDateTime

data class CouponIssueRequestMessage(
    val eventId: String,
    val requestId: Long,
    val couponId: Long,
    val userId: Long,
    val requestedAt: ZonedDateTime,
)
