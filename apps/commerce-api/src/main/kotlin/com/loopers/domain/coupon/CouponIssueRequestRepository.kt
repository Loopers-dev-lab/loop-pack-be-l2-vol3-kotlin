package com.loopers.domain.coupon

interface CouponIssueRequestRepository {
    fun save(couponIssueRequest: CouponIssueRequest): CouponIssueRequest
    fun findByRequestId(requestId: String): CouponIssueRequest?
}
