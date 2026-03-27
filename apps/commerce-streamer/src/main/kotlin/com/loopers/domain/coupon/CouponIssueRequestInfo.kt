package com.loopers.domain.coupon

data class CouponIssueRequestInfo(
    val id: Long,
    val requestId: String,
    val couponId: Long,
    val userId: Long,
    val status: CouponIssueRequestStatus,
)
