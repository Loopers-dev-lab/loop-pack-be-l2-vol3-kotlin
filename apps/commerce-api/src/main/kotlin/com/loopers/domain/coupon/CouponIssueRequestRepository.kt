package com.loopers.domain.coupon

interface CouponIssueRequestRepository {
    fun save(couponIssueRequest: CouponIssueRequest): CouponIssueRequest
    fun findByRequestId(requestId: String): CouponIssueRequest?
    fun findByRequestIdAndUserId(requestId: String, userId: Long): CouponIssueRequest?
}
