package com.loopers.infrastructure.outbox

data class CouponIssueRequestedOutboxMessagePayload(
    val requestId: Long,
    val couponId: Long,
    val userId: Long,
)
