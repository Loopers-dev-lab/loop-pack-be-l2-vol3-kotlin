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
