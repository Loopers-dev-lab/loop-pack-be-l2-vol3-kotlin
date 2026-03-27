package com.loopers.domain.fcfscoupon

interface FcfsCouponIssueRequestRepository {
    fun save(request: FcfsCouponIssueRequestModel): FcfsCouponIssueRequestModel
    fun findById(id: Long): FcfsCouponIssueRequestModel?
}
