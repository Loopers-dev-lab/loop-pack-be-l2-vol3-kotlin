package com.loopers.application.coupon

import com.loopers.infrastructure.coupon.CouponIssueRequestEntity
import com.loopers.infrastructure.coupon.CouponIssueRequestJpaRepository
import com.loopers.infrastructure.coupon.CouponJpaRepository
import com.loopers.infrastructure.coupon.IssuedCouponEntity
import com.loopers.infrastructure.coupon.IssuedCouponJpaRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime

@Component
class CouponIssueRequestProcessor(
    private val couponIssueRequestJpaRepository: CouponIssueRequestJpaRepository,
    private val couponJpaRepository: CouponJpaRepository,
    private val issuedCouponJpaRepository: IssuedCouponJpaRepository,
) {
    @Transactional
    fun process(requestId: Long, couponId: Long, memberId: Long) {
        val request = couponIssueRequestJpaRepository.findById(requestId)
            .orElse(null)
            ?: return

        if (request.status != "PENDING") {
            return
        }

        if (issuedCouponJpaRepository.existsByCouponIdAndMemberId(couponId, memberId)) {
            request.markFailed("FAILED_DUPLICATE", "이미 발급받은 쿠폰입니다.")
            couponIssueRequestJpaRepository.save(request)
            return
        }

        if (couponJpaRepository.tryIncreaseIssuedCount(couponId) == 0) {
            request.markFailed("FAILED_SOLD_OUT", "선착순 쿠폰이 모두 소진되었습니다.")
            couponIssueRequestJpaRepository.save(request)
            return
        }

        val issuedCoupon = issuedCouponJpaRepository.save(
            IssuedCouponEntity(
                couponId = couponId,
                memberId = memberId,
                status = "AVAILABLE",
                issuedAt = ZonedDateTime.now(),
            ),
        )
        request.status = "SUCCEEDED"
        request.issuedCouponId = issuedCoupon.id
        request.failureReason = null
        couponIssueRequestJpaRepository.save(request)
    }

    private fun CouponIssueRequestEntity.markFailed(status: String, reason: String) {
        this.status = status
        this.failureReason = reason
        this.issuedCouponId = null
    }
}
