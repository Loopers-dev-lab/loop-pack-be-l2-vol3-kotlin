package com.loopers.domain.coupon

enum class CouponIssueRequestStatus {
    PENDING,
    SUCCEEDED,
    FAILED_DUPLICATE,
    FAILED_SOLD_OUT,
    FAILED_INVALID,
}
