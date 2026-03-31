package com.loopers.application.fcfscoupon

interface FcfsCouponIssueRequestPublisher {
    fun publish(requestId: Long, templateId: Long, memberId: Long)
}
