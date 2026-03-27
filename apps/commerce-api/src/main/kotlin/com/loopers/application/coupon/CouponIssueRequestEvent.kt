package com.loopers.application.coupon

data class CouponIssueRequestEvent(
    val requestId: String,
    val couponId: Long,
    val userId: Long,
)
