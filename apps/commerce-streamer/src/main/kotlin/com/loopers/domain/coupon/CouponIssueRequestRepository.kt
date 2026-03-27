package com.loopers.domain.coupon

interface CouponIssueRequestRepository {
    fun findByRequestId(requestId: String): CouponIssueRequestInfo?
    fun markFailed(id: Long, reason: String)
    fun markSuccess(id: Long)
}
