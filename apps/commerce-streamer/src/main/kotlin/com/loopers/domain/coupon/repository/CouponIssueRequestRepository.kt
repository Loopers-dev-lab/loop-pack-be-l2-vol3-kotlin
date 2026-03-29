package com.loopers.domain.coupon.repository

import com.loopers.domain.coupon.model.CouponIssueRequest

interface CouponIssueRequestRepository {
    fun findByRequestId(requestId: String): CouponIssueRequest?
    fun save(request: CouponIssueRequest): CouponIssueRequest
}
