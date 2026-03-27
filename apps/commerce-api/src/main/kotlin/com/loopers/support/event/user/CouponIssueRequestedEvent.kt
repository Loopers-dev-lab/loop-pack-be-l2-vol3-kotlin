package com.loopers.support.event.user

import org.springframework.context.ApplicationEvent

class CouponIssueRequestedEvent(
    val requestId: Long,
    val couponId: Long,
    val userId: Long,
) : ApplicationEvent(requestId)
