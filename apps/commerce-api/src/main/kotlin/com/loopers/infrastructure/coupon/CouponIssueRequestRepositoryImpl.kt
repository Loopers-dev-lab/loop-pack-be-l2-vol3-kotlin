package com.loopers.infrastructure.coupon

import com.loopers.domain.coupon.CouponIssueRequest
import com.loopers.domain.coupon.CouponIssueRequestRepository
import org.springframework.stereotype.Repository

@Repository
class CouponIssueRequestRepositoryImpl(
    private val jpaRepository: CouponIssueRequestJpaRepository,
) : CouponIssueRequestRepository {
    override fun save(request: CouponIssueRequest): Long {
        val entity = CouponIssueRequestMapper.toEntity(request)
        val saved = jpaRepository.save(entity)
        return requireNotNull(saved.id) { "CouponIssueRequest 저장 실패: id가 생성되지 않았습니다." }
    }

    override fun findByRequestId(requestId: String): CouponIssueRequest? {
        return jpaRepository.findByRequestId(requestId)
            ?.let { CouponIssueRequestMapper.toDomain(it) }
    }
}
