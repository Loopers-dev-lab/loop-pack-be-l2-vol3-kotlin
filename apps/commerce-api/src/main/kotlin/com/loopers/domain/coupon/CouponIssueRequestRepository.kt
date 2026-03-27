package com.loopers.domain.coupon

interface CouponIssueRequestRepository {
    fun save(couponIssueRequest: CouponIssueRequestModel): CouponIssueRequestModel
    fun findByIdAndDeletedAtIsNull(id: Long): CouponIssueRequestModel?
    fun findByIdForUpdate(id: Long): CouponIssueRequestModel?
}
