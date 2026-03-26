package com.loopers.domain.coupon

interface CouponIssueRequestRepository {
    fun save(request: CouponIssueRequestModel): CouponIssueRequestModel
    fun findByCouponIdAndUserId(couponId: Long, userId: Long): CouponIssueRequestModel?
    fun findById(id: Long): CouponIssueRequestModel?
}
