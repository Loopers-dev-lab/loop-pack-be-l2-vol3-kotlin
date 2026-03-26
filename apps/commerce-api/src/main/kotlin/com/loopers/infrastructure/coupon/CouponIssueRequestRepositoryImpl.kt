package com.loopers.infrastructure.coupon

import com.loopers.domain.coupon.CouponIssueRequest
import com.loopers.domain.coupon.CouponIssueRequestRepository
import org.springframework.stereotype.Component

@Component
class CouponIssueRequestRepositoryImpl(
    private val couponIssueRequestJpaRepository: CouponIssueRequestJpaRepository,
    private val couponIssueRequestMapper: CouponIssueRequestMapper,
) : CouponIssueRequestRepository {
    override fun save(request: CouponIssueRequest): CouponIssueRequest {
        val entity = if (request.id == null) {
            couponIssueRequestMapper.toEntity(request)
        } else {
            couponIssueRequestJpaRepository.findById(request.id)
                .map {
                    couponIssueRequestMapper.update(it, request)
                    it
                }
                .orElseGet { couponIssueRequestMapper.toEntity(request) }
        }
        return couponIssueRequestMapper.toDomain(couponIssueRequestJpaRepository.save(entity))
    }

    override fun findById(id: Long): CouponIssueRequest? {
        return couponIssueRequestJpaRepository.findById(id)
            .map(couponIssueRequestMapper::toDomain)
            .orElse(null)
    }
}
