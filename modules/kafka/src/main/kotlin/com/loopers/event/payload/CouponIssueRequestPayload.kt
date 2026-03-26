package com.loopers.event.payload

data class CouponIssueRequestPayload(
    val couponId: Long,
    val userId: Long,
    val requestId: String,
)
