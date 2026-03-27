package com.loopers.domain.coupon

interface CouponIssueRequestRepository {
    fun save(request: CouponIssueRequest): CouponIssueRequest

    fun findById(id: Long): CouponIssueRequest?

    fun findByIdAndUserId(id: Long, userId: Long): CouponIssueRequest?

    fun findByCouponIdAndUserId(couponId: Long, userId: Long): CouponIssueRequest?
}
