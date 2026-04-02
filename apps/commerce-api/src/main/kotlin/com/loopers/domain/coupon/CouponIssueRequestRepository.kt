package com.loopers.domain.coupon

interface CouponIssueRequestRepository {
    fun save(request: CouponIssueRequest): CouponIssueRequest
    fun findByRequestIdOrNull(requestId: String): CouponIssueRequest?
}
