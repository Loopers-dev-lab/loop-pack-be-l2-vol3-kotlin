package com.loopers.infrastructure.coupon

import com.loopers.domain.coupon.CouponIssueRequestModel
import com.loopers.domain.coupon.CouponIssueRequestRepository
import org.springframework.stereotype.Component

@Component
class CouponIssueRequestRepositoryImpl(
    private val couponIssueRequestJpaRepository: CouponIssueRequestJpaRepository,
) : CouponIssueRequestRepository {
    override fun save(couponIssueRequest: CouponIssueRequestModel): CouponIssueRequestModel {
        return couponIssueRequestJpaRepository.save(couponIssueRequest)
    }

    override fun findByIdAndDeletedAtIsNull(id: Long): CouponIssueRequestModel? {
        return couponIssueRequestJpaRepository.findByIdAndDeletedAtIsNull(id)
    }

    override fun findByIdForUpdate(id: Long): CouponIssueRequestModel? {
        return couponIssueRequestJpaRepository.findByIdForUpdate(id)
    }
}
