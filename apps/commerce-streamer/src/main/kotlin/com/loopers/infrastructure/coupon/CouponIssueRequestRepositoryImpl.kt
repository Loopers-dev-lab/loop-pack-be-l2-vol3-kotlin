package com.loopers.infrastructure.coupon

import com.loopers.domain.coupon.CouponIssueRequest
import com.loopers.domain.coupon.CouponIssueRequestRepository
import org.springframework.stereotype.Repository

@Repository
class CouponIssueRequestRepositoryImpl(
    private val couponIssueRequestJpaRepository: CouponIssueRequestJpaRepository,
    private val couponIssueRequestMapper: CouponIssueRequestMapper,
) : CouponIssueRequestRepository {
    override fun save(request: CouponIssueRequest): CouponIssueRequest {
        return couponIssueRequestMapper.toDomain(
            couponIssueRequestJpaRepository.saveAndFlush(couponIssueRequestMapper.toEntity(request)),
        )
    }

    override fun findById(id: Long): CouponIssueRequest? {
        return couponIssueRequestJpaRepository.findByIdAndDeletedAtIsNull(id)
            ?.let { couponIssueRequestMapper.toDomain(it) }
    }

    override fun findByIdForUpdate(id: Long): CouponIssueRequest? {
        return couponIssueRequestJpaRepository.findByIdForUpdate(id)
            ?.let { couponIssueRequestMapper.toDomain(it) }
    }

    override fun findByIdAndUserId(id: Long, userId: Long): CouponIssueRequest? {
        return couponIssueRequestJpaRepository.findByIdAndUserIdAndDeletedAtIsNull(id, userId)
            ?.let { couponIssueRequestMapper.toDomain(it) }
    }

    override fun findByCouponIdAndUserId(couponId: Long, userId: Long): CouponIssueRequest? {
        return couponIssueRequestJpaRepository.findByCouponIdAndUserIdAndDeletedAtIsNull(couponId, userId)
            ?.let { couponIssueRequestMapper.toDomain(it) }
    }
}
