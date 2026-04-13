package com.loopers.interfaces.consumer.dto

data class CouponIssuePayload(
    val eventId: String,
    val eventType: String,
    val couponId: Long,
    val userId: Long,
)
