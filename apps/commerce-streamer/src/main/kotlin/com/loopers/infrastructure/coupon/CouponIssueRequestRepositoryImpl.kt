package com.loopers.infrastructure.coupon

import com.loopers.domain.coupon.CouponIssueRequestInfo
import com.loopers.domain.coupon.CouponIssueRequestRepository
import com.loopers.domain.coupon.CouponIssueRequestStatus
import org.springframework.stereotype.Repository
import java.time.ZonedDateTime

@Repository
class CouponIssueRequestRepositoryImpl(
    private val jpaRepository: CouponIssueRequestJpaRepository,
) : CouponIssueRequestRepository {

    override fun findByRequestId(requestId: String): CouponIssueRequestInfo? {
        return jpaRepository.findByRequestId(requestId)?.let { entity ->
            CouponIssueRequestInfo(
                id = requireNotNull(entity.id),
                requestId = entity.requestId,
                couponId = entity.couponId,
                userId = entity.userId,
                status = CouponIssueRequestStatus.valueOf(entity.status),
            )
        }
    }

    override fun markFailed(id: Long, reason: String) {
        val entity = jpaRepository.findById(id).orElseThrow()
        val updated = CouponIssueRequestEntity(
            id = entity.id,
            requestId = entity.requestId,
            couponId = entity.couponId,
            userId = entity.userId,
            status = "FAILED",
            failureReason = reason,
            processedAt = ZonedDateTime.now(),
        )
        jpaRepository.save(updated)
    }

    override fun markSuccess(id: Long) {
        val entity = jpaRepository.findById(id).orElseThrow()
        val updated = CouponIssueRequestEntity(
            id = entity.id,
            requestId = entity.requestId,
            couponId = entity.couponId,
            userId = entity.userId,
            status = "SUCCESS",
            failureReason = null,
            processedAt = ZonedDateTime.now(),
        )
        jpaRepository.save(updated)
    }
}
