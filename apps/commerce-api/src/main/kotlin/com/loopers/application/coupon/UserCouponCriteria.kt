package com.loopers.application.coupon

data class IssueCouponCriteria(
    val loginId: String,
    val couponId: Long,
)

data class GetMyCouponsCriteria(
    val loginId: String,
    val page: Int,
    val size: Int,
)

data class RequestCouponIssueCriteria(
    val loginId: String,
    val couponId: Long,
)

data class GetCouponIssueStatusCriteria(
    val loginId: String,
    val requestId: Long,
)
