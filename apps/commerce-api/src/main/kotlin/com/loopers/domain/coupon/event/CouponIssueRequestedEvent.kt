package com.loopers.domain.coupon.event

import java.time.ZonedDateTime
import java.util.UUID

data class CouponIssueRequestedEvent(
    val userId: Long,
    val templateId: Long,
    val requestedAt: ZonedDateTime = ZonedDateTime.now(),
    val dedupeKey: String = "coupon.issue.requested:$userId:$templateId:${UUID.randomUUID()}",
    val type: String = "CouponIssueRequestedEvent",
)
