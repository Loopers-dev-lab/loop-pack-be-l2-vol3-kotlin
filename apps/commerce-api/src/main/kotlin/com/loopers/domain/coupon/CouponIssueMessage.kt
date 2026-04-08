package com.loopers.domain.coupon

data class CouponIssueMessage(
    val requestId: String,
    val userId: Long,
    val couponTemplateId: Long,
)
