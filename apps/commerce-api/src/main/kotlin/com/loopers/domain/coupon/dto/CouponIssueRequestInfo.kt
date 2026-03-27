package com.loopers.domain.coupon.dto

data class CouponIssueRequestInfo(
    val templateId: Long,
    val dedupeKey: String,
    val message: String = "쿠폰 발급 요청이 접수되었습니다.",
)
