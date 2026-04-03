package com.loopers.domain.coupon

interface CouponIssueRequestRepository {
    fun save(request: CouponIssueRequest): Long
    fun findByRequestId(requestId: String): CouponIssueRequest?
}
