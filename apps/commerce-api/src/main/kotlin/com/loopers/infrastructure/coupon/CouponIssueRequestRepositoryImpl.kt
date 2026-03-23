package com.loopers.infrastructure.coupon

import com.loopers.domain.coupon.model.CouponIssueRequest
import com.loopers.domain.coupon.repository.CouponIssueRequestRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

interface CouponIssueRequestJpaRepository : JpaRepository<CouponIssueRequestEntity, Long> {
    fun findByRequestId(requestId: String): CouponIssueRequestEntity?
    fun findByRequestIdAndUserId(requestId: String, userId: Long): CouponIssueRequestEntity?
}

@Repository
class CouponIssueRequestRepositoryImpl(
    private val couponIssueRequestJpaRepository: CouponIssueRequestJpaRepository,
) : CouponIssueRequestRepository {

    override fun save(request: CouponIssueRequest): CouponIssueRequest =
        couponIssueRequestJpaRepository.save(CouponIssueRequestEntity.fromDomain(request)).toDomain()

    override fun findByRequestId(requestId: String): CouponIssueRequest? =
        couponIssueRequestJpaRepository.findByRequestId(requestId)?.toDomain()

    override fun findByRequestIdAndUserId(requestId: String, userId: Long): CouponIssueRequest? =
        couponIssueRequestJpaRepository.findByRequestIdAndUserId(requestId, userId)?.toDomain()
}
